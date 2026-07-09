package com.example.scoi.global.redis;

import com.example.scoi.global.config.RedisConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RedisUtil#increment} 이 실제 Redis에서 INCR과 TTL 설정을 한 번의 스크립트로 수행하는지 검증한다.
 * 단위 테스트는 RedisUtil을 모킹하므로 Lua 스크립트 자체는 여기서만 실증된다.
 * Redis가 떠 있지 않으면 테스트 전체를 건너뛴다.
 */
@EnabledIf("redisAvailable")
class RedisUtilIntegrationTest {

    private static final String HOST = "localhost";
    private static final int PORT = 6379;
    private static final String KEY = "test:redisutil:increment";

    private static LettuceConnectionFactory connectionFactory;
    private static RedisTemplate<String, String> redisTemplate;
    private static RedisUtil redisUtil;

    static boolean redisAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, PORT), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeAll
    static void setUp() {
        connectionFactory = new LettuceConnectionFactory(HOST, PORT);
        connectionFactory.afterPropertiesSet();

        RedisConfig config = new RedisConfig();
        redisTemplate = config.redisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        redisUtil = new RedisUtil(redisTemplate, config.incrementWithTtlScript());
    }

    @AfterAll
    static void tearDown() {
        connectionFactory.destroy();
    }

    @AfterEach
    void cleanUp() {
        redisTemplate.delete(KEY);
    }

    @Test
    @DisplayName("최초 증가: 1을 반환하고 TTL이 설정된다")
    void firstIncrementSetsTtl() {
        long count = redisUtil.increment(KEY, 5, TimeUnit.MINUTES);

        assertThat(count).isEqualTo(1L);
        assertThat(redisTemplate.getExpire(KEY, TimeUnit.SECONDS))
                .isPositive()
                .isLessThanOrEqualTo(300L);
    }

    @Test
    @DisplayName("재증가: 값은 늘어나지만 TTL은 초기화되지 않는다 (고정 창)")
    void subsequentIncrementDoesNotResetTtl() {
        redisUtil.increment(KEY, 5, TimeUnit.MINUTES);
        // 첫 증가가 건 TTL을 짧게 덮어써 두면, 재증가가 TTL을 다시 5분으로 늘리는지 확인할 수 있다
        redisTemplate.expire(KEY, 30, TimeUnit.SECONDS);

        long count = redisUtil.increment(KEY, 5, TimeUnit.MINUTES);

        assertThat(count).isEqualTo(2L);
        assertThat(redisTemplate.getExpire(KEY, TimeUnit.SECONDS)).isLessThanOrEqualTo(30L);
    }

    @Test
    @DisplayName("저장된 값은 정수 문자열이라 get()으로 읽을 수 있다")
    void valueIsReadableAsString() {
        redisUtil.increment(KEY, 5, TimeUnit.MINUTES);
        redisUtil.increment(KEY, 5, TimeUnit.MINUTES);

        assertThat(redisUtil.get(KEY)).isEqualTo("2");
    }
}
