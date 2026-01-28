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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
                CoolSmsDTO.SendRequest smsRequest = new CoolSmsDTO.SendRequest(
                    request.phoneNumber(),
                    fromNumber,
                    "[SCOI] 인증번호: " + verificationCode
                );
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

    @Transactional
    public AuthResDTO.SignupResponse signup(AuthReqDTO.SignupRequest request) {
        // 1. Verification Token 검증
        String tokenKey = VERIFICATION_PREFIX + request.verificationToken();
        String verifiedPhoneNumber = redisUtil.get(tokenKey);

        if (verifiedPhoneNumber == null) {
            throw new AuthException(AuthErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if (!verifiedPhoneNumber.equals(request.phoneNumber())) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }

        // 2. 중복 체크
        if (memberRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new AuthException(AuthErrorCode.ALREADY_REGISTERED_PHONE);
        }

        // 3. 간편비밀번호 BCrypt 해싱 (평문 → 해시)
        String hashedPassword = passwordEncoder.encode(request.simplePassword());

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

        // 5. Verification Token 삭제 (일회용)
        redisUtil.delete(tokenKey);

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

        // 3. 비밀번호 검증 (평문 vs BCrypt 해시)
        if (!passwordEncoder.matches(request.simplePassword(), member.getSimplePassword())) {
            member.increaseLoginFailCount();
            memberRepository.save(member);
            log.warn("로그인 실패: phoneNumber={}, failCount={}", request.phoneNumber(), member.getLoginFailCount());
            throw new AuthException(AuthErrorCode.INVALID_PASSWORD);
        }

        // 4. 성공 처리
        member.resetLoginFailCount();
        member.updateLastLoginAt(LocalDateTime.now());
        memberRepository.save(member);

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
        return new AuthResDTO.LoginResponse(accessToken, refreshToken);
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

        // 7. 기존 RT 삭제 (Rotation)
        memberTokenRepository.delete(memberToken);

        // 8. 새 RT 저장 (issuedAt은 기존 값 유지)
        MemberToken newMemberToken = MemberToken.builder()
            .member(memberToken.getMember())
            .refreshToken(newRefreshToken)
            .issuedAt(issuedAt)  // 최초 발급 시간 유지!
            .expirationDate(now.plusDays(REFRESH_TOKEN_SLIDING_DAYS))
            .build();

        memberTokenRepository.save(newMemberToken);

        log.info("토큰 재발급 성공: phoneNumber={}", phoneNumber);
        return new AuthResDTO.ReissueResponse(newAccessToken, newRefreshToken);
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