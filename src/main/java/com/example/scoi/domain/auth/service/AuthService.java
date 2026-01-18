package com.example.scoi.domain.auth.service;

import com.example.scoi.domain.auth.dto.AuthReqDTO;
import com.example.scoi.domain.auth.dto.AuthResDTO;
import com.example.scoi.domain.auth.exception.AuthException;
import com.example.scoi.domain.auth.exception.code.AuthErrorCode;
import com.example.scoi.domain.member.entity.Member;
import com.example.scoi.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    // TODO: 다음 PR에서 추가 예정
    // private final MemberTokenRepository memberTokenRepository;
    // private final RedisService redisService;
    // private final JwtUtil jwtUtil;
    // private final BCryptPasswordEncoder passwordEncoder;
    // private final SmsService smsService;

    /**
     * SMS 발송
     */
    public void sendSms(AuthReqDTO.SmsSendRequest request) {
        String phoneNumber = request.phoneNumber();

        // TODO: 1. 인증번호 생성 (6자리)
        // TODO: 2. Redis에 저장 (3분 TTL) - SMS 인증번호는 Redis 사용
        // TODO: 3. SMS 발송 (CoolSMS 등)

        log.info("[SMS 발송] 휴대폰: {}", phoneNumber);
    }

    /**
     * SMS 검증 (result: null, isExistingMember 삭제)
     */
    public AuthResDTO.SmsVerifyResponse verifySms(AuthReqDTO.SmsVerifyRequest request) {
        String phoneNumber = request.phoneNumber();
        String verificationCode = request.verificationCode();

        // TODO: 1. Redis에서 인증번호 조회
        // TODO: 2. 일치 여부 확인
        // TODO: 3. 일치하면 Redis에서 삭제

        log.info("[SMS 검증] 휴대폰: {}, 인증번호: {}", phoneNumber, verificationCode);

        return new AuthResDTO.SmsVerifyResponse();
    }

    /**
     * 회원가입 (result: null)
     */
    @Transactional
    public void signup(AuthReqDTO.SignupRequest request) {
        String phoneNumber = request.phoneNumber();

        // 1. 중복 체크 (409로 기존회원 판단)
        if (memberRepository.existsByPhoneNumber(phoneNumber)) {
            throw new AuthException(AuthErrorCode.ALREADY_REGISTERED_PHONE);
        }

        // TODO: 2. 영문 이름 대문자 변환 (서버에서 자동 처리)
        // TODO: 3. 간편비밀번호 BCrypt 암호화
        // TODO: 4. Member 엔티티 생성 및 저장

        log.info("[회원가입] 휴대폰: {}, 이름: {}", phoneNumber, request.koreanName());
    }

    /**
     * 로그인 (isBioRegistered 삭제)
     */
    @Transactional
    public AuthResDTO.LoginResponse login(AuthReqDTO.LoginRequest request) {
        String phoneNumber = request.phoneNumber();
        String simplePassword = request.simplePassword();

        // 1. 회원 조회
        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new AuthException(AuthErrorCode.MEMBER_NOT_FOUND));

        // 2. 로그인 실패 5회 체크
        if (member.getLoginFailCount() >= 5) {
            // TODO: RT 블랙리스트 등록 (Redis)
            throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED);
        }

        // TODO: 3. 비밀번호 검증 (BCrypt)
        // TODO: 4. 로그인 실패 시 실패 횟수 증가
        // TODO: 5. 로그인 성공 시 실패 횟수 초기화, lastLoginAt 업데이트
        // TODO: 6. Access Token, Refresh Token 생성
        // TODO: 7. Refresh Token DB 저장 (MemberToken 테이블, 만료기간: 비활성 14일/최대 30일)

        log.info("[로그인 성공] 휴대폰: {}", phoneNumber);

        // 임시 반환
        return new AuthResDTO.LoginResponse("tempAccessToken", "tempRefreshToken");
    }

    /**
     * 토큰 재발급 (AT + RT 둘 다 재발급)
     */
    @Transactional
    public AuthResDTO.ReissueResponse reissue(AuthReqDTO.ReissueRequest request) {
        String refreshToken = request.refreshToken();

        // TODO: 1. Refresh Token 검증
        // TODO: 2. DB에서 RT 조회 및 일치 확인 (MemberToken 테이블)
        // TODO: 3. 만료 여부 확인 (expirationDate)
        // TODO: 4. 새로운 AT, RT 생성
        // TODO: 5. 기존 RT 삭제, 새 RT DB 저장 (RT 회전)

        log.info("[토큰 재발급]");

        // 임시 반환
        return new AuthResDTO.ReissueResponse("newAccessToken", "newRefreshToken");
    }

    /**
     * 로그아웃 (블랙리스트 처리)
     */
    @Transactional
    public void logout(String phoneNumber, String accessToken) {
        // TODO: 다음 PR에서 구현
        // 1. DB에서 RT 삭제 (MemberToken 테이블)
        // 2. Redis 블랙리스트에 Access Token 등록 (남은 시간만큼 TTL)

        log.info("[로그아웃] 휴대폰: {}", phoneNumber);
    }
}