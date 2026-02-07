package com.example.scoi.global.config;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Configuration
public class CoolSmsConfig {

    @Value("${coolsms.api-key}")
    private String apiKey;

    @Value("${coolsms.api-secret}")
    private String apiSecret;

    /**
     * CoolSMS 전용 RequestInterceptor
     * ⚠️ @Bean을 제거하여 전역 Bean이 되지 않도록 함
     * Feign Client의 configuration 속성으로만 사용됨
     */
    public RequestInterceptor coolSmsRequestInterceptor() {
        return template -> {
            // CoolSMS API URL로 시작하는 요청에만 적용
            if (template.url() != null && template.url().contains("coolsms")) {
                String date = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                String salt = UUID.randomUUID().toString().replace("-", "");
                String signature = generateSignature(date, salt);

                template.header("Authorization",
                    String.format("HMAC-SHA256 apiKey=%s, date=%s, salt=%s, signature=%s",
                        apiKey, date, salt, signature));
                template.header("Content-Type", "application/json");
            }
        };
    }

    private String generateSignature(String date, String salt) {
        try {
            String message = date + salt;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("HMAC-SHA256 서명 생성 실패: {}", e.getMessage());
            throw new RuntimeException("Failed to generate HMAC-SHA256 signature", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
