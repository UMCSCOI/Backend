package com.example.scoi.global.config;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Configuration
public class CoolSmsConfig {

    @Value("${coolsms.api-key}")
    private String apiKey;

    @Value("${coolsms.api-secret}")
    private String apiSecret;

    @Bean
    public RequestInterceptor coolSmsRequestInterceptor() {
        return template -> {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String signature = generateSignature(timestamp);

            template.header("Authorization", "HMAC-SHA256 apiKey=" + apiKey + ", date=" + timestamp + ", signature=" + signature);
            template.header("Content-Type", "application/json");
        };
    }

    private String generateSignature(String timestamp) {
        try {
            String message = timestamp + apiKey;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("HMAC-SHA256 서명 생성 실패: {}", e.getMessage());
            throw new RuntimeException("Failed to generate HMAC-SHA256 signature", e);
        }
    }
}
