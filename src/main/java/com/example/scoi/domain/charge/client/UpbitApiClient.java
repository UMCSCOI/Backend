package com.example.scoi.domain.charge.client;

import com.example.scoi.domain.charge.dto.ChargeResDTO;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.global.util.JwtApiUtil;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpbitApiClient implements ExchangeApiClient {
    
    private final RestTemplate restTemplate;
    private final JwtApiUtil jwtApiUtil;
    private final ObjectMapper objectMapper;
    
    @Override
    public ChargeResDTO.BalanceDTO getBalance(String phoneNumber, ExchangeType exchangeType) {
        try {
            // ✅ JWT 토큰 생성 (Bearer 포함)
            // GET /v1/accounts는 쿼리 파라미터 없음
            String authorization = jwtApiUtil.createUpBitJwt(phoneNumber, null, null);
            log.info("업비트 API 호출 시작 - phoneNumber: {}", phoneNumber);
            log.debug("업비트 JWT 토큰: {}", authorization);
            
            // ✅ HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authorization);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // ✅ 실제 업비트 API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                "https://api.upbit.com/v1/accounts",
                HttpMethod.GET,
                entity,
                String.class
            );
            
            log.info("업비트 API 응답 상태: {}", response.getStatusCode());
            log.debug("업비트 API 응답 본문: {}", response.getBody());
            
            // ✅ 응답 파싱 및 변환
            return parseBalanceResponse(response.getBody());
            
        } catch (GeneralSecurityException e) {
            log.error("업비트 JWT 생성 실패", e);
            throw new RuntimeException("JWT 생성 실패", e);
        } catch (Exception e) {
            log.error("업비트 API 호출 실패", e);
            throw new RuntimeException("업비트 API 호출 실패: " + e.getMessage(), e);
        }
    }
    
    private ChargeResDTO.BalanceDTO parseBalanceResponse(String responseBody) {
        try {
            // 실제 업비트 API 응답 형식에 맞게 파싱
            // 예시 응답: [{"currency": "KRW", "balance": "100000.0", "locked": "0.0", ...}, ...]
            
            List<Map<String, Object>> accounts = objectMapper.readValue(
                responseBody, 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            if (accounts.isEmpty()) {
                log.error("업비트 계좌 정보가 없습니다.");
                throw new RuntimeException("계좌 정보가 없습니다.");
            }
            
            // KRW 계좌 찾기
            Map<String, Object> krwAccount = accounts.stream()
                    .filter(account -> "KRW".equals(account.get("currency")))
                    .findFirst()
                    .orElse(accounts.get(0));  // 없으면 첫 번째 계좌 사용
            
            log.info("업비트 계좌 조회 완료 - currency: {}, balance: {}, locked: {}", 
                    krwAccount.get("currency"), 
                    krwAccount.get("balance"), 
                    krwAccount.get("locked"));
            
            return ChargeResDTO.BalanceDTO.builder()
                    .currency(String.valueOf(krwAccount.get("currency")))
                    .balance(String.valueOf(krwAccount.get("balance")))
                    .locked(String.valueOf(krwAccount.get("locked")))
                    .build();
                    
        } catch (Exception e) {
            log.error("업비트 API 응답 파싱 실패: {}", responseBody, e);
            throw new RuntimeException("응답 파싱 실패: " + e.getMessage(), e);
        }
    }
}