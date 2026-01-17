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
            //  JWT 토큰 생성 (Bearer 포함)
            String authorization = jwtApiUtil.createBithumbJwt(phoneNumber, null, null);
            log.info("빗썸 API 호출 시작 - phoneNumber: {}", phoneNumber);
            log.info("빗썸 JWT 토큰: {}", authorization); // DEBUG -> INFO로 변경 (토큰 확인용)
            
            //  HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authorization);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            //  실제 빗썸 API 2.0 호출 (전체 계좌 조회)
            ResponseEntity<String> response = restTemplate.exchange(
                "https://api.bithumb.com/v1/accounts",
                HttpMethod.GET,
                entity,
                String.class
            );
            
            log.info("빗썸 API 응답 상태: {}", response.getStatusCode());
            log.debug("빗썸 API 응답 본문: {}", response.getBody());
            
            // 응답 파싱 및 변환
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
            // 빗썸 API 2.0 응답 형식에 맞게 파싱
  
            
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            // 배열 형식인 경우
            if (jsonNode.isArray()) {
                // KRW 계좌 찾기
                for (JsonNode account : jsonNode) {
                    String currency = account.has("currency") ? account.get("currency").asText() : null;
                    if ("KRW".equals(currency)) {
                        String balance = account.has("balance") ? account.get("balance").asText() : "0";
                        String locked = account.has("locked") ? account.get("locked").asText() : "0";
                        
                        return ChargeResDTO.BalanceDTO.builder()
                                .currency("KRW")
                                .balance(balance)
                                .locked(locked)
                                .build();
                    }
                }
                // KRW가 없으면 첫 번째 계좌 사용
                if (jsonNode.size() > 0) {
                    JsonNode firstAccount = jsonNode.get(0);
                    String currency = firstAccount.has("currency") ? firstAccount.get("currency").asText() : "KRW";
                    String balance = firstAccount.has("balance") ? firstAccount.get("balance").asText() : "0";
                    String locked = firstAccount.has("locked") ? firstAccount.get("locked").asText() : "0";
                    
                    return ChargeResDTO.BalanceDTO.builder()
                            .currency(currency)
                            .balance(balance)
                            .locked(locked)
                            .build();
                }
            }
            
            // 구 API 형식 (status, data 구조) - 하위 호환성
            if (jsonNode.has("status") && jsonNode.has("data")) {
                String status = jsonNode.get("status").asText();
                if (!"0000".equals(status)) {
                    log.error("빗썸 API 에러 응답: {}", responseBody);
                    throw new RuntimeException("빗썸 API 에러: " + status);
                }
                
                JsonNode data = jsonNode.get("data");
                String totalKrw = data.has("total_krw") ? data.get("total_krw").asText() : "0";
                String inUseKrw = data.has("in_use_krw") ? data.get("in_use_krw").asText() : "0";
                String availableKrw = data.has("available_krw") ? data.get("available_krw").asText() : "0";
                
                return ChargeResDTO.BalanceDTO.builder()
                        .currency("KRW")
                        .balance(availableKrw)
                        .locked(inUseKrw)
                        .build();
            }
            
            throw new RuntimeException("빗썸 API 응답 형식을 파싱할 수 없습니다.");
                    
        } catch (Exception e) {
            log.error("빗썸 API 응답 파싱 실패: {}", responseBody, e);
            throw new RuntimeException("응답 파싱 실패: " + e.getMessage(), e);
        }
    }
}