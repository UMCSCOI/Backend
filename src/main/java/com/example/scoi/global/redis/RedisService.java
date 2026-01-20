package com.example.scoi.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String SMS_PREFIX = "sms:";
    private static final String VERIFICATION_PREFIX = "verification:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private static final long SMS_CODE_TTL = 3; // 3분
    private static final long VERIFICATION_TOKEN_TTL = 10; // 10분

    // SMS 인증번호 저장
    public void saveSmsCode(String phoneNumber, String code) {
        String key = SMS_PREFIX + phoneNumber;
        redisTemplate.opsForValue().set(key, code, SMS_CODE_TTL, TimeUnit.MINUTES);
    }

    // SMS 인증번호 조회
    public String getSmsCode(String phoneNumber) {
        String key = SMS_PREFIX + phoneNumber;
        return redisTemplate.opsForValue().get(key);
    }

    // SMS 인증번호 삭제
    public void deleteSmsCode(String phoneNumber) {
        String key = SMS_PREFIX + phoneNumber;
        redisTemplate.delete(key);
    }

    // Verification Token 저장
    public void saveVerificationToken(String token, String phoneNumber) {
        String key = VERIFICATION_PREFIX + token;
        redisTemplate.opsForValue().set(key, phoneNumber, VERIFICATION_TOKEN_TTL, TimeUnit.MINUTES);
    }

    // Verification Token으로 전화번호 조회
    public String getPhoneNumberByToken(String token) {
        String key = VERIFICATION_PREFIX + token;
        return redisTemplate.opsForValue().get(key);
    }

    // Verification Token 삭제
    public void deleteVerificationToken(String token) {
        String key = VERIFICATION_PREFIX + token;
        redisTemplate.delete(key);
    }

    // Access Token 블랙리스트 등록
    public void addToBlacklist(String accessToken, long remainingTimeMillis) {
        String key = BLACKLIST_PREFIX + accessToken;
        redisTemplate.opsForValue().set(key, "logout", remainingTimeMillis, TimeUnit.MILLISECONDS);
    }

    // 블랙리스트 확인
    public boolean isBlacklisted(String accessToken) {
        String key = BLACKLIST_PREFIX + accessToken;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
