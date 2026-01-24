package com.example.scoi.domain.auth.service;

import com.example.scoi.domain.auth.dto.AuthReqDTO;
import com.example.scoi.domain.auth.dto.AuthResDTO;
import com.example.scoi.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    
    // TODO: 다음 PR에서 추가 예정
    // private final JwtUtil jwtUtil;
    // private final RedisUtil redisUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthResDTO.SmsSendResponse sendSms(AuthReqDTO.SmsSendRequest request) {
        // TODO: SMS 발송 구현
        // 1. 인증번호 생성 (6자리)
        // 2. Redis에 저장 (key: phoneNumber, value: code, TTL: 3분)
        // 3. CoolSMS API 호출
        // 4. expiredAt 반환 (현재시간 + 3분)
        return null;
    }

    public AuthResDTO.SmsVerifyResponse verifySms(AuthReqDTO.SmsVerifyRequest request) {
        // TODO: SMS 검증 구현
        // 1. Redis에서 인증번호 조회
        // 2. 일치 여부 확인
        // 3. 일치하면 Redis에서 삭제
        // 4. result: null 반환
        return null;
    }

    public AuthResDTO.SignupResponse signup(AuthReqDTO.SignupRequest request) {
        // TODO: 회원가입 구현
        // 1. SMS 인증 완료 여부 확인 (Redis)
        // 2. 전화번호 중복 체크 (409 에러)
        // 3. 영문 이름 대문자 변환
        // 4. 간편비밀번호 BCrypt 암호화
        // 5. Member 저장
        // 6. memberId, koreanName 반환
        return null;
    }

    public AuthResDTO.LoginResponse login(AuthReqDTO.LoginRequest request) {
        // TODO: 로그인 구현
        // 1. 전화번호로 Member 조회
        // 2. 로그인 실패 5회 체크
        //    if (member.getLoginFailCount() >= 5) {
        //        throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED);
        //    }
        // 3. BCrypt 비밀번호 검증
        // 4. 실패 시: loginFailCount++, 성공 시: loginFailCount=0, lastLoginAt 업데이트
        // 5. JWT 토큰 생성 (AT, RT)
        // 6. RT를 DB(member_token)에 저장
        // 7. AT, RT 반환
        return null;
    }

    public AuthResDTO.ReissueResponse reissue(AuthReqDTO.ReissueRequest request) {
        // TODO: 토큰 재발급 구현
        // 1. RT 검증
        // 2. DB에서 RT 조회 (member_token)
        // 3. RT 만료 확인
        // 4. 새 AT, RT 생성
        // 5. 기존 RT 삭제 (회전)
        // 6. 새 RT DB 저장
        // 7. 새 AT, RT 반환
        return null;
    }

    public void logout(String phoneNumber, String accessToken) {
        // TODO: 로그아웃 구현
        // 1. DB에서 RT 삭제 (member_token 테이블에서 phoneNumber로 조회 후 삭제)
        // 2. Redis에 AT 블랙리스트 등록 (key: accessToken, TTL: AT 남은 시간)
        //    ※ RT는 DB에서 삭제되므로 재발급 불가 → 별도 블랙리스트 불필요
    }
}