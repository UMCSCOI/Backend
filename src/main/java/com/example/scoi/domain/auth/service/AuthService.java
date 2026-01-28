package com.example.scoi.domain.auth.service;

import com.example.scoi.domain.auth.dto.AuthReqDTO;
import com.example.scoi.domain.auth.dto.AuthResDTO;
import com.example.scoi.domain.auth.exception.AuthException;
import com.example.scoi.domain.auth.exception.code.AuthErrorCode;
import com.example.scoi.domain.member.entity.Member;
import com.example.scoi.domain.member.entity.MemberToken;
import com.example.scoi.domain.member.repository.MemberRepository;
import com.example.scoi.domain.member.repository.MemberTokenRepository;
import com.example.scoi.global.client.CoolSmsClient;
import com.example.scoi.global.client.dto.CoolSmsDTO;
import com.example.scoi.global.redis.RedisUtil;
import com.example.scoi.global.security.jwt.JwtUtil;
import com.example.scoi.global.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final MemberTokenRepository memberTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final CoolSmsClient coolSmsClient;
    private final HashUtil hashUtil;

    @Value("${coolsms.from-number}")
    private String fromNumber;

    @Value("${coolsms.enabled:true}")
    private boolean smsEnabled;

    @Value("${coolsms.expose-code:false}")
    private boolean exposeCode;

    // Redis 키 접두사
    private static final String SMS_PREFIX = "sms:";
    private static final String VERIFICATION_PREFIX = "verification:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    // 상수
    private static final int SMS_CODE_LENGTH = 6;
    private static final long SMS_EXPIRATION_MINUTES = 5;
    private static final long VERIFICATION_EXPIRATION_MINUTES = 10;
    private static final long REFRESH_TOKEN_SLIDING_DAYS = 14;  // 비활성 기준 만료
    private static final long REFRESH_TOKEN_ABSOLUTE_DAYS = 30; // 최대 수명

    public AuthResDTO.SmsSendResponse sendSms(AuthReqDTO.SmsSendRequest request) {
        // 1. 인증번호 생성 (6자리)
        String verificationCode = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));

        // 2. Redis 저장
        String redisKey = SMS_PREFIX + request.phoneNumber();
        redisUtil.set(redisKey, verificationCode, SMS_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        // 3. CoolSMS 발송
        if (smsEnabled) {
            try {
                CoolSmsDTO.Message message = new CoolSmsDTO.Message(
                    request.phoneNumber(),
                    fromNumber,
                    "[SCOI] 인증번호: " + verificationCode
                );
                CoolSmsDTO.SendRequest smsRequest = new CoolSmsDTO.SendRequest(message);
                coolSmsClient.sendMessage(smsRequest);
                log.info("SMS 발송 성공: phoneNumber={}, code={}", request.phoneNumber(), verificationCode);
            } catch (Exception e) {
                log.error("SMS 발송 실패: {}", e.getMessage());
                throw new AuthException(AuthErrorCode.SMS_SEND_FAILED);
            }
        } else {
            log.warn("[DEV/QA MODE] SMS 발송 건너뛰기: phoneNumber={}, code={}", request.phoneNumber(), verificationCode);
        }

        // 4. 응답
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(SMS_EXPIRATION_MINUTES);
        String codeToExpose = exposeCode ? verificationCode : null;
        return new AuthResDTO.SmsSendResponse(expiredAt, codeToExpose);
    }

    public AuthResDTO.SmsVerifyResponse verifySms(AuthReqDTO.SmsVerifyRequest request) {
        // 1. Redis 조회
        String redisKey = SMS_PREFIX + request.phoneNumber();
        String storedCode = redisUtil.get(redisKey);

        if (storedCode == null) {
            throw new AuthException(AuthErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        // 2. 검증
        if (!storedCode.equals(request.verificationCode())) {
            throw new AuthException(AuthErrorCode.INVALID_VERIFICATION_CODE);
        }

        // 3. SMS 코드 삭제
        redisUtil.delete(redisKey);

        // 4. Verification Token 발급
        String verificationToken = jwtUtil.createVerificationToken(request.phoneNumber());

        // 5. Redis 저장 (10분 TTL)
        String tokenKey = VERIFICATION_PREFIX + verificationToken;
        redisUtil.set(tokenKey, request.phoneNumber(), VERIFICATION_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        log.info("SMS 인증 성공: phoneNumber={}", request.phoneNumber());
        return new AuthResDTO.SmsVerifyResponse(verificationToken);
    }

    /**
     * Verification Token 검증 (범용)
     * SMS 인증 완료 후 발급된 토큰이 유효한지 검증합니다.
     * 다른 도메인(회원가입, 비밀번호 찾기, 휴대폰 번호 변경 등)에서 재사용 가능합니다.
     *
     * @param verificationToken SMS 인증 완료 후 발급받은 토큰
     * @param phoneNumber 검증할 휴대폰 번호
     * @return 검증된 휴대폰 번호
     * @throws AuthException 토큰이 만료되었거나 휴대폰 번호가 일치하지 않을 경우
     */
    public String validateVerificationToken(String verificationToken, String phoneNumber) {
        String tokenKey = VERIFICATION_PREFIX + verificationToken;
        String verifiedPhoneNumber = redisUtil.get(tokenKey);

        if (verifiedPhoneNumber == null) {
            throw new AuthException(AuthErrorCode.VERIFICATION_TOKEN_EXPIRED);
        }

        if (!verifiedPhoneNumber.equals(phoneNumber)) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }

        log.debug("Verification Token 검증 성공: phoneNumber={}", phoneNumber);
        return verifiedPhoneNumber;
    }

    @Transactional
    public AuthResDTO.SignupResponse signup(AuthReqDTO.SignupRequest request) {
        // 1. Verification Token 검증 (SMS 인증 완료 확인)
        validateVerificationToken(request.verificationToken(), request.phoneNumber());

        // 2. 중복 체크
        if (memberRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new AuthException(AuthErrorCode.ALREADY_REGISTERED_PHONE);
        }

        // 3. 간편비밀번호 AES 복호화 후 검증 및 BCrypt 해싱 (AES 암호문 → 평문 → 검증 → BCrypt 해시)
        String rawPassword;
        try {
            rawPassword = new String(hashUtil.decryptAES(request.simplePassword()));
        } catch (GeneralSecurityException e) {
            log.error("AES 복호화 실패: phoneNumber={}", request.phoneNumber(), e);
            throw new AuthException(AuthErrorCode.INVALID_PASSWORD);
        }

        // 복호화된 평문이 6자리 숫자인지 검증
        if (!rawPassword.matches("^\\d{6}$")) {
            log.warn("간편비밀번호 형식 오류: phoneNumber={}", request.phoneNumber());
            throw new AuthException(AuthErrorCode.INVALID_PASSWORD);
        }

        String hashedPassword = passwordEncoder.encode(rawPassword);

        // 4. Member 생성
        Member member = Member.builder()
            .phoneNumber(request.phoneNumber())
            .englishName(request.englishName().toUpperCase())
            .koreanName(request.koreanName())
            .residentNumber(request.residentNumber())
            .simplePassword(hashedPassword)
            .memberType(request.memberType())
            .build();

        memberRepository.save(member);

        log.info("회원가입 성공: memberId={}, phoneNumber={}", member.getId(), member.getPhoneNumber());
        return new AuthResDTO.SignupResponse(member.getId(), member.getKoreanName());
    }

    @Transactional
    public AuthResDTO.LoginResponse login(AuthReqDTO.LoginRequest request) {
        // 1. 회원 조회
        Member member = memberRepository.findByPhoneNumber(request.phoneNumber())
            .orElseThrow(() -> new AuthException(AuthErrorCode.MEMBER_NOT_FOUND));

        // 2. 계정 잠금 체크
        if (member.getLoginFailCount() >= 5) {
            throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED);
        }

        // 3. 비밀번호 AES 복호화 후 검증 (AES 암호문 → 평문 → 형식 검증 → BCrypt 해시와 비교)
        String rawPassword;
        try {
            rawPassword = new String(hashUtil.decryptAES(request.simplePassword()));
        } catch (GeneralSecurityException e) {
            log.error("AES 복호화 실패: phoneNumber={}", request.phoneNumber(), e);
            member.increaseLoginFailCount();
            int failCount = member.getLoginFailCount();
            int remainingAttempts = Math.max(5 - failCount, 0);
            throw new AuthException(
                AuthErrorCode.INVALID_PASSWORD,
                Map.of(
                    "loginFailCount", String.valueOf(failCount),
                    "remainingAttempts", String.valueOf(remainingAttempts)
                )
            );
        }

        // 복호화된 평문이 6자리 숫자인지 검증
        if (!rawPassword.matches("^\\d{6}$")) {
            log.warn("간편비밀번호 형식 오류: phoneNumber={}", request.phoneNumber());
            member.increaseLoginFailCount();
            int failCount = member.getLoginFailCount();
            int remainingAttempts = Math.max(5 - failCount, 0);
            throw new AuthException(
                AuthErrorCode.INVALID_PASSWORD,
                Map.of(
                    "loginFailCount", String.valueOf(failCount),
                    "remainingAttempts", String.valueOf(remainingAttempts)
                )
            );
        }

        if (!passwordEncoder.matches(rawPassword, member.getSimplePassword())) {
            member.increaseLoginFailCount();
            int failCount = member.getLoginFailCount();
            int remainingAttempts = Math.max(5 - failCount, 0);
            log.warn("로그인 실패: phoneNumber={}, failCount={}, remainingAttempts={}",
                request.phoneNumber(), failCount, remainingAttempts);
            throw new AuthException(
                AuthErrorCode.INVALID_PASSWORD,
                Map.of(
                    "loginFailCount", String.valueOf(failCount),
                    "remainingAttempts", String.valueOf(remainingAttempts)
                )
            );
        }

        // 4. 성공 처리 (JPA 더티 체킹으로 자동 저장)
        member.resetLoginFailCount();
        member.updateLastLoginAt(LocalDateTime.now());

        // 5. 토큰 생성
        String accessToken = jwtUtil.createAccessToken(request.phoneNumber());
        String refreshToken = jwtUtil.createRefreshToken(request.phoneNumber());

        // 6. RT DB 저장 (기존 있으면 삭제 후 재생성)
        memberTokenRepository.deleteByMemberPhoneNumber(request.phoneNumber());

        LocalDateTime now = LocalDateTime.now();
        MemberToken memberToken = MemberToken.builder()
                .member(member)
                .refreshToken(refreshToken)
                .issuedAt(now)  // 최초 발급 시간
                .expirationDate(now.plusDays(REFRESH_TOKEN_SLIDING_DAYS))
                .build();

        memberTokenRepository.save(memberToken);

        log.info("로그인 성공: phoneNumber={}", request.phoneNumber());
        return new AuthResDTO.LoginResponse(
                accessToken,
                refreshToken,
                jwtUtil.getAccessTokenExpirationInSeconds()
        );
    }

    @Transactional
    public AuthResDTO.ReissueResponse reissue(AuthReqDTO.ReissueRequest request) {
        // 1. RT 검증
        if (!jwtUtil.validateToken(request.refreshToken())) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }

        // 2. phoneNumber 추출
        String phoneNumber = jwtUtil.getPhoneNumberFromToken(request.refreshToken());

        // 3. DB 조회
        MemberToken memberToken = memberTokenRepository.findByRefreshToken(request.refreshToken())
            .orElseThrow(() -> new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));

        // 4. 만료 확인 (Sliding Expiration)
        LocalDateTime now = LocalDateTime.now();
        if (memberToken.getExpirationDate().isBefore(now)) {
            memberTokenRepository.delete(memberToken);
            throw new AuthException(AuthErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        // 5. 최대 수명 확인 (Absolute Expiration)
        LocalDateTime issuedAt = memberToken.getIssuedAt();
        LocalDateTime absoluteExpiration = issuedAt.plusDays(REFRESH_TOKEN_ABSOLUTE_DAYS);
        if (absoluteExpiration.isBefore(now)) {
            memberTokenRepository.delete(memberToken);
            log.warn("RT 최대 수명 만료: phoneNumber={}, issuedAt={}", phoneNumber, issuedAt);
            throw new AuthException(AuthErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        // 6. 새 토큰 생성
        String newAccessToken = jwtUtil.createAccessToken(phoneNumber);
        String newRefreshToken = jwtUtil.createRefreshToken(phoneNumber);

        // 7. RT 업데이트 (Rotation, issuedAt 갱신하여 최대 수명도 연장)
        memberToken.updateTokenWithIssuedAt(newRefreshToken, now.plusDays(REFRESH_TOKEN_SLIDING_DAYS), now);

        // 8. lastLoginAt 갱신 (사용자 활동 추적)
        Member member = memberToken.getMember();
        member.updateLastLoginAt(now);

        log.info("토큰 재발급 성공: phoneNumber={}", phoneNumber);
        return new AuthResDTO.ReissueResponse(
                newAccessToken,
                newRefreshToken,
                jwtUtil.getAccessTokenExpirationInSeconds()
        );
    }

    @Transactional
    public void logout(String phoneNumber, String accessToken) {
        // 1. RT 삭제
        memberTokenRepository.deleteByMemberPhoneNumber(phoneNumber);

        // 2. AT 블랙리스트 등록
        long remainingTime = jwtUtil.getRemainingTime(accessToken);

        if (remainingTime > 0) {
            String blacklistKey = BLACKLIST_PREFIX + accessToken;
            redisUtil.set(blacklistKey, "logout", remainingTime, TimeUnit.MILLISECONDS);
        }

        log.info("로그아웃 성공: phoneNumber={}", phoneNumber);
    }
}