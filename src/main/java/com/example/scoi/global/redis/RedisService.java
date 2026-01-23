package com.example.scoi.global.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String SMS_PREFIX = "sms:";
    private static final String VERIFICATION_PREFIX = "verification:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private static final long SMS_CODE_TTL = 3; // 3분
    private static final long VERIFICATION_TOKEN_TTL = 10; // 10분

    private static final String BLACKLIST_VALUE = "logout";

    // SMS 인증번호 저장
    public void saveSmsCode(String phoneNumber, String code) {
        validateInput(phoneNumber, "phoneNumber");
        validateInput(code, "code");
        
        String key = buildKey(SMS_PREFIX, phoneNumber);
        redisTemplate.opsForValue().set(key, code, SMS_CODE_TTL, TimeUnit.MINUTES);
        log.debug("SMS 인증번호 저장: {}", key);
    }

    // SMS 인증번호 조회
    public String getSmsCode(String phoneNumber) {
        validateInput(phoneNumber, "phoneNumber");
        
        String key = buildKey(SMS_PREFIX, phoneNumber);
        return redisTemplate.opsForValue().get(key);
    }

    // SMS 인증번호 삭제
    public void deleteSmsCode(String phoneNumber) {
        validateInput(phoneNumber, "phoneNumber");
        
        String key = buildKey(SMS_PREFIX, phoneNumber);
        redisTemplate.delete(key);
        log.debug("SMS 인증번호 삭제: {}", key);
    }

    // Verification Token 저장
    public void saveVerificationToken(String token, String phoneNumber) {
        validateInput(token, "token");
        validateInput(phoneNumber, "phoneNumber");
        
        String key = buildKey(VERIFICATION_PREFIX, token);
        redisTemplate.opsForValue().set(key, phoneNumber, VERIFICATION_TOKEN_TTL, TimeUnit.MINUTES);
        log.debug("Verification Token 저장: {}", key);
    }

    // Verification Token으로 전화번호 조회
    public String getPhoneNumberByToken(String token) {
        validateInput(token, "token");
        
        String key = buildKey(VERIFICATION_PREFIX, token);
        return redisTemplate.opsForValue().get(key);
    }

    // Verification Token 삭제
    public void deleteVerificationToken(String token) {
        validateInput(token, "token");
        
        String key = buildKey(VERIFICATION_PREFIX, token);
        redisTemplate.delete(key);
        log.debug("Verification Token 삭제: {}", key);
    }

    // Access Token 블랙리스트 등록
    public void addToBlacklist(String accessToken, long remainingTimeMillis) {
        validateInput(accessToken, "accessToken");
        
        if (remainingTimeMillis <= 0) {
            log.debug("남은 시간이 0 이하이므로 블랙리스트 등록 생략: {}", remainingTimeMillis);
            return;
        }
        
        String key = buildKey(BLACKLIST_PREFIX, accessToken);
        redisTemplate.opsForValue().set(key, BLACKLIST_VALUE, remainingTimeMillis, TimeUnit.MILLISECONDS);
        log.debug("블랙리스트 등록: {} (TTL: {}ms)", key, remainingTimeMillis);
    }

    // 블랙리스트 확인
    public boolean isBlacklisted(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return false;
        }
        
        String key = buildKey(BLACKLIST_PREFIX, accessToken);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Redis Key 생성 (중복 제거)
     */
    private String buildKey(String prefix, String value) {
        return prefix + value;
    }

    /**
     * 입력값 검증
     */
    private void validateInput(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + "는 null이거나 빈 문자열일 수 없습니다.");
        }
    }
}
