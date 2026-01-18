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
public class BinanceApiClient implements ExchangeApiClient {
    
    private final RestTemplate restTemplate;
    private final JwtApiUtil jwtApiUtil;
    private final ObjectMapper objectMapper;
    
    @Override
    public ChargeResDTO.BalanceDTO getBalance(String phoneNumber, ExchangeType exchangeType) {
        // TODO: 바이낸스 API 구현 예정 - JwtApiUtil에 createBinanceAuth 메서드 추가 필요
        throw new RuntimeException("바이낸스 API는 아직 구현되지 않았습니다.");
        
        /* 임시 주석 처리 - 바이낸스 구현 시 주석 해제
        try {
            // 바이낸스 API 호출
            // 참고: https://developers.binance.com/docs/wallet/asset/user-assets
            // 바이낸스는 HMAC-SHA256 서명 방식을 사용
            // jwtApiUtil에도 추가해야 함. 아직 못함

            
            log.info("바이낸스 API 호출 시작 - phoneNumber: {}", phoneNumber);
            
            // HMAC-SHA256 서명 생성 (JwtApiUtil 사용)
            JwtApiUtil.BinanceApiAuthInfo authInfo = jwtApiUtil.createBinanceAuth(phoneNumber);
            log.debug("바이낸스 API Key: {}, timestamp: {}, signature: {}", 
                    authInfo.getApiKey(), authInfo.getTimestamp(), authInfo.getSignature());
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-MBX-APIKEY", authInfo.getApiKey()); // 바이낸스 API Key 헤더
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 바이낸스 API 호출 (POST 요청, query string에 timestamp와 signature 포함)
            // 참고: 바이낸스 API는 POST 요청도 query string으로 파라미터를 전달합니다.
            String url = "https://api.binance.com/sapi/v3/asset/getUserAsset?" + authInfo.getQueryString();
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );
            
            log.info("바이낸스 API 응답 상태: {}", response.getStatusCode());
            log.debug("바이낸스 API 응답 본문: {}", response.getBody());
            
            // 응답 파싱 및 변환
            return parseBalanceResponse(response.getBody());
            
        } catch (GeneralSecurityException e) {
            log.error("바이낸스 HMAC-SHA256 서명 생성 실패", e);
            throw new RuntimeException("HMAC-SHA256 서명 생성 실패", e);
        } catch (Exception e) {
            log.error("바이낸스 API 호출 실패", e);
            throw new RuntimeException("바이낸스 API 호출 실패: " + e.getMessage(), e);
        }
        */
    }
    
    private ChargeResDTO.BalanceDTO parseBalanceResponse(String responseBody) {
        try {
            // 바이낸스 API 응답 형식에 맞게 파싱
            // 참고: https://developers.binance.com/docs/wallet/asset/user-assets
            
            List<Map<String, Object>> assets = objectMapper.readValue(
                responseBody,
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            if (assets.isEmpty()) {
                log.error("바이낸스 자산 정보가 없습니다.");
                throw new RuntimeException("자산 정보가 없습니다.");
            }
            
            // USDT 자산 찾기 (또는 기본 화폐)
            Map<String, Object> usdtAsset = assets.stream()
                    .filter(asset -> "USDT".equals(asset.get("asset")))
                    .findFirst()
                    .orElse(assets.get(0));  // 없으면 첫 번째 자산 사용
            
            String asset = String.valueOf(usdtAsset.get("asset"));
            String free = String.valueOf(usdtAsset.get("free"));
            String locked = String.valueOf(usdtAsset.get("locked"));
            
            log.info("바이낸스 자산 조회 완료 - asset: {}, free: {}, locked: {}", asset, free, locked);
            
            return ChargeResDTO.BalanceDTO.builder()
                    .currency(asset)
                    .balance(free)
                    .locked(locked)
                    .build();
                    
        } catch (Exception e) {
            log.error("바이낸스 API 응답 파싱 실패: {}", responseBody, e);
            throw new RuntimeException("응답 파싱 실패: " + e.getMessage(), e);
        }
    }
}