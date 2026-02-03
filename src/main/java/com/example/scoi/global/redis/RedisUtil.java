package com.example.scoi.global.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * Redis 범용 유틸리티 클래스
 * key, value, ttl만 받아서 Redis CRUD 수행
 * 비즈니스 로직(prefix 조합, TTL 설정 등)은 Service 계층에서 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Redis에 데이터 저장 (TTL 포함)
     */
    public void set(String key, String value, long timeout, TimeUnit unit) {
        validateInput(key);
        validateInput(value);
        
        redisTemplate.opsForValue().set(key, value, timeout, unit);
        log.debug("Redis 저장: key={}, TTL={}{}", key, timeout, unit);
    }

    /**
     * Redis에 데이터 저장 (TTL 없음 - 영구 저장)
     */
    public void set(String key, String value) {
        validateInput(key);
        validateInput(value);
        
        redisTemplate.opsForValue().set(key, value);
        log.debug("Redis 저장(영구): key={}", key);
    }

    /**
     * Redis에서 데이터 조회
     */
    public String get(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Redis에서 데이터 삭제
     */
    public void delete(String key) {
        validateInput(key);
        
        redisTemplate.delete(key);
        log.debug("Redis 삭제: key={}", key);
    }

    /**
     * Redis에 키가 존재하는지 확인
     */
    public boolean exists(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 입력값 검증
     */
    private void validateInput(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(value + "는 null이거나 빈 문자열일 수 없습니다.");
        }
    }
}
