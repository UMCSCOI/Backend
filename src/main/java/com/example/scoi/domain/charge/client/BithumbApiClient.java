package com.example.scoi.domain.charge.client;

import com.example.scoi.domain.charge.dto.ChargeResDTO;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.global.util.JwtApiUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.security.GeneralSecurityException;

@Component
@RequiredArgsConstructor
@Slf4j
public class BithumbApiClient implements ExchangeApiClient {
    
    private final RestTemplate restTemplate;
    private final JwtApiUtil jwtApiUtil;
    private final ObjectMapper objectMapper;
    
    @Override
    public ChargeResDTO.BalanceDTO getBalance(String phoneNumber, ExchangeType exchangeType) {
        try {
            // ✅ JWT 토큰 생성 (Bearer 포함)
            String authorization = jwtApiUtil.createBithumbJwt(phoneNumber, null, null);
            log.info("빗썸 API 호출 시작 - phoneNumber: {}", phoneNumber);
            log.debug("빗썸 JWT 토큰: {}", authorization);
            
            // ✅ HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authorization);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // ✅ 실제 빗썸 API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                "https://api.bithumb.com/info/balance",
                HttpMethod.GET,
                entity,
                String.class
            );
            
            log.info("빗썸 API 응답 상태: {}", response.getStatusCode());
            log.debug("빗썸 API 응답 본문: {}", response.getBody());
            
            // ✅ 응답 파싱 및 변환
            return parseBalanceResponse(response.getBody());
            
        } catch (GeneralSecurityException e) {
            log.error("빗썸 JWT 생성 실패", e);
            throw new RuntimeException("JWT 생성 실패", e);
        } catch (Exception e) {
            log.error("빗썸 API 호출 실패", e);
            throw new RuntimeException("빗썸 API 호출 실패: " + e.getMessage(), e);
        }
    }
    
    private ChargeResDTO.BalanceDTO parseBalanceResponse(String responseBody) {
        try {
            // 실제 빗썸 API 응답 형식에 맞게 파싱
            // 예시 응답: {"status": "0000", "data": {"total_krw": "100000", "in_use_krw": "0", "available_krw": "100000"}}
            
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String status = jsonNode.get("status").asText();
            
            if (!"0000".equals(status)) {
                log.error("빗썸 API 에러 응답: {}", responseBody);
                throw new RuntimeException("빗썸 API 에러: " + status);
            }
            
            JsonNode data = jsonNode.get("data");
            if (data == null) {
                throw new RuntimeException("빗썸 API 응답에 data가 없습니다.");
            }
            
            String totalKrw = data.has("total_krw") ? data.get("total_krw").asText() : "0";
            String inUseKrw = data.has("in_use_krw") ? data.get("in_use_krw").asText() : "0";
            String availableKrw = data.has("available_krw") ? data.get("available_krw").asText() : "0";
            
            return ChargeResDTO.BalanceDTO.builder()
                    .currency("KRW")
                    .balance(availableKrw)  // 사용 가능한 잔액
                    .locked(inUseKrw)       // 사용 중인 잔액 (주문에 묶인 금액)
                    .build();
                    
        } catch (Exception e) {
            log.error("빗썸 API 응답 파싱 실패: {}", responseBody, e);
            throw new RuntimeException("응답 파싱 실패: " + e.getMessage(), e);
        }
    }
}