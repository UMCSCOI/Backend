package com.example.scoi.domain.auth.service;

import com.example.scoi.domain.auth.dto.AuthReqDTO;
import com.example.scoi.domain.auth.dto.AuthResDTO;
import com.example.scoi.domain.auth.exception.AuthException;
import com.example.scoi.domain.auth.exception.code.AuthErrorCode;
import com.example.scoi.domain.member.repository.MemberRepository;
import com.example.scoi.global.redis.RedisUtil;
import com.example.scoi.global.security.jwt.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SMS 인증 무차별 대입 방어 로직 단위 테스트.
 * Spring 컨텍스트 없이 Mockito로 협력 객체를 스텁하여 분기별 동작을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private AuthService authService;

    private static final String PHONE = "01012345678";
    private static final String CODE = "123456";
    private static final String SMS_KEY = "sms:" + PHONE;
    private static final String FAIL_KEY = "sms:verify:fail:" + PHONE;
    private static final String COOLDOWN_KEY = "sms:cooldown:" + PHONE;

    private AuthReqDTO.SmsVerifyRequest request(String verificationCode) {
        return new AuthReqDTO.SmsVerifyRequest(PHONE, verificationCode);
    }

    @Nested
    @DisplayName("verifySms - SMS 인증번호 검증")
    class VerifySms {

        @Test
        @DisplayName("성공: 코드 일치 시 토큰 발급 및 코드·실패카운터 삭제")
        void success() {
            // given
            when(redisUtil.get(SMS_KEY)).thenReturn(CODE);
            when(jwtUtil.createVerificationToken(PHONE)).thenReturn("verification-token");
            when(memberRepository.existsByPhoneNumber(PHONE)).thenReturn(true);

            // when
            AuthResDTO.SmsVerifyResponse response = authService.verifySms(request(CODE));

            // then
            assertThat(response.verificationToken()).isEqualTo("verification-token");
            assertThat(response.isExistingMember()).isTrue();
            verify(redisUtil).delete(SMS_KEY);   // 코드 삭제
            verify(redisUtil).delete(FAIL_KEY);  // 실패 카운터 초기화
        }

        @Test
        @DisplayName("실패: 저장된 코드가 없으면 VERIFICATION_CODE_EXPIRED, 실패 카운터 증가 안 함")
        void expired() {
            // given
            when(redisUtil.get(SMS_KEY)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> authService.verifySms(request(CODE)))
                    .isInstanceOf(AuthException.class)
                    .extracting(e -> ((AuthException) e).getCode())
                    .isEqualTo(AuthErrorCode.VERIFICATION_CODE_EXPIRED);

            verify(redisUtil, never()).increment(any(), anyLong(), any());
        }

        @Test
        @DisplayName("실패: 코드 불일치 - 한도 미만이면 INVALID_VERIFICATION_CODE + 남은 횟수 반환")
        void mismatchUnderLimit() {
            // given: 실패 카운터가 2 (한도 5 미만)
            when(redisUtil.get(SMS_KEY)).thenReturn(CODE);
            when(redisUtil.increment(eq(FAIL_KEY), eq(5L), eq(TimeUnit.MINUTES))).thenReturn(2L);

            // when & then
            assertThatThrownBy(() -> authService.verifySms(request("000000")))
                    .isInstanceOf(AuthException.class)
                    .satisfies(e -> {
                        AuthException ex = (AuthException) e;
                        assertThat(ex.getCode()).isEqualTo(AuthErrorCode.INVALID_VERIFICATION_CODE);
                        assertThat(ex.getBind()).containsEntry("remainingAttempts", "3"); // 5 - 2
                    });

            // 한도 미만이므로 코드·카운터 삭제하지 않음
            verify(redisUtil, never()).delete(SMS_KEY);
            verify(redisUtil, never()).delete(FAIL_KEY);
        }

        @Test
        @DisplayName("실패: 코드 불일치 - 한도 도달 시 VERIFICATION_ATTEMPTS_EXCEEDED + 코드·카운터 삭제")
        void mismatchLimitExceeded() {
            // given: 실패 카운터가 5 (한도 도달)
            when(redisUtil.get(SMS_KEY)).thenReturn(CODE);
            when(redisUtil.increment(eq(FAIL_KEY), eq(5L), eq(TimeUnit.MINUTES))).thenReturn(5L);

            // when & then
            assertThatThrownBy(() -> authService.verifySms(request("000000")))
                    .isInstanceOf(AuthException.class)
                    .extracting(e -> ((AuthException) e).getCode())
                    .isEqualTo(AuthErrorCode.VERIFICATION_ATTEMPTS_EXCEEDED);

            // 코드 무효화: 코드·카운터 모두 삭제
            verify(redisUtil).delete(SMS_KEY);
            verify(redisUtil).delete(FAIL_KEY);
        }
    }

    @Nested
    @DisplayName("sendSms - SMS 인증번호 발송")
    class SendSms {

        @Test
        @DisplayName("새 코드 발급 시 이전 실패 카운터를 초기화한다")
        void resetsFailCounter() {
            // given: 쿨다운 없음 (smsEnabled 기본값 false → 실제 발송 건너뜀)
            when(redisUtil.exists(COOLDOWN_KEY)).thenReturn(false);

            // when
            authService.sendSms(new AuthReqDTO.SmsSendRequest(PHONE));

            // then: 새 코드에는 새 시도 예산이 주어져야 한다
            verify(redisUtil).set(eq(SMS_KEY), any(), eq(5L), eq(TimeUnit.MINUTES));
            verify(redisUtil).delete(FAIL_KEY);
        }

        @Test
        @DisplayName("쿨다운 중이면 SMS_COOLDOWN, 실패 카운터를 건드리지 않는다")
        void cooldown() {
            // given
            when(redisUtil.exists(COOLDOWN_KEY)).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.sendSms(new AuthReqDTO.SmsSendRequest(PHONE)))
                    .isInstanceOf(AuthException.class)
                    .extracting(e -> ((AuthException) e).getCode())
                    .isEqualTo(AuthErrorCode.SMS_COOLDOWN);

            verify(redisUtil, never()).delete(FAIL_KEY);
        }
    }
}
