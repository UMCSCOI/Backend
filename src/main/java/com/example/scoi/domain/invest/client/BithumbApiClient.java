package com.example.scoi.domain.invest.client;

import com.example.scoi.domain.invest.dto.InvestResDTO;
import com.example.scoi.domain.invest.exception.InvestException;
import com.example.scoi.domain.invest.exception.code.InvestErrorCode;
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

import java.math.BigDecimal;
import java.security.GeneralSecurityException;

@Component
@RequiredArgsConstructor
@Slf4j
public class BithumbApiClient implements ExchangeApiClient {
    
    private final RestTemplate restTemplate;
    private final JwtApiUtil jwtApiUtil;
    private final ObjectMapper objectMapper;
    
    @Override
    public InvestResDTO.MaxOrderInfoDTO getMaxOrderInfo(String phoneNumber, ExchangeType exchangeType, String coinType) {
        try {
            // JWT 토큰 생성 (Bearer 포함)
            String authorization = jwtApiUtil.createBithumbJwt(phoneNumber, null, null);
            log.info("빗썸 최대 주문 정보 조회 API 호출 시작 - phoneNumber: {}, coinType: {}", phoneNumber, coinType);
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authorization);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
         
            // coinType 변환 필요 (KRW-BTC -> BTC_KRW 등)
            String url = "https://api.bithumb.com/v1/balance?currency=" + convertCoinType(coinType);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            log.info("빗썸 최대 주문 정보 조회 API 응답 상태: {}", response.getStatusCode());
            log.debug("빗썸 최대 주문 정보 조회 API 응답 본문: {}", response.getBody());
            
            // 응답 파싱 및 변환
            return parseMaxOrderInfoResponse(response.getBody(), coinType);
            
        } catch (GeneralSecurityException e) {
            log.error("빗썸 JWT 생성 실패", e);
            throw new RuntimeException("JWT 생성 실패", e);
        } catch (Exception e) {
            log.error("빗썸 최대 주문 정보 조회 API 호출 실패", e);
            throw new RuntimeException("빗썸 API 호출 실패: " + e.getMessage(), e);
        }
    }
    
   
    //coinType 변환 (업비트 형식 -> 빗썸 형식)
    private String convertCoinType(String coinType) {
        // TODO: 실제 변환 로직 구현 필요
        // 업비트: KRW-BTC, 빗썸: BTC_KRW
        if (coinType.startsWith("KRW-")) {
            String coin = coinType.substring(4); // KRW- 제거
            return coin + "_KRW";
        }
        return coinType;
    }
    
    private InvestResDTO.MaxOrderInfoDTO parseMaxOrderInfoResponse(String responseBody, String coinType) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            

            
            String balance = "0";
            if (jsonNode.has("status") && jsonNode.has("data")) {
                String status = jsonNode.get("status").asText();
                if (!"0000".equals(status)) {
                    log.error("빗썸 API 에러 응답: {}", responseBody);
                    throw new RuntimeException("빗썸 API 에러: " + status);
                }
                
                JsonNode data = jsonNode.get("data");
                // coinType에 따라 available 필드 선택
                if (coinType.contains("KRW")) {
                    balance = data.has("available_krw") ? data.get("available_krw").asText() : "0";
                } else {
                    String coin = coinType.replace("KRW-", "").toLowerCase();
                    String availableField = "available_" + coin;
                    balance = data.has(availableField) ? data.get(availableField).asText() : "0";
                }
            }
            
            return InvestResDTO.MaxOrderInfoDTO.builder()
                    .balance(balance)
                    .build();
                    
        } catch (Exception e) {
            log.error("빗썸 최대 주문 정보 조회 API 응답 파싱 실패: {}", responseBody, e);
            throw new RuntimeException("응답 파싱 실패: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void checkOrderAvailability(
            String phoneNumber,
            ExchangeType exchangeType,
            String market,
            String side,
            String orderType,
            String price,
            String volume
    ) {
        try {
            log.info("빗썸 주문 가능 여부 확인 API 호출 시작 - phoneNumber: {}, market: {}, side: {}", 
                    phoneNumber, market, side);
            
            // 1. 주문 가능 정보 조회 
            JsonNode orderChance = getOrderChance(phoneNumber, market);
            
            // 2. 주문 가능 여부 검증
            validateOrderAvailability(market, side, orderType, price, volume, orderChance);
            
            log.info("빗썸 주문 가능 여부 확인 완료 - 주문 가능");
            
        } catch (InvestException e) {
            throw e;
        } catch (Exception e) {
            log.error("빗썸 주문 가능 여부 확인 API 호출 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }
   
    //주문 가능 정보 조회
    private JsonNode getOrderChance(String phoneNumber, String market) {
        try {
            // JWT 토큰 생성 (query parameter 포함)
            // 빗썸 market 형식 변환 필요 (KRW-BTC -> BTC_KRW 등)
            String convertedMarket = convertMarketForBithumb(market);
            String query = "market=" + convertedMarket;
            String authorization = jwtApiUtil.createBithumbJwt(phoneNumber, query, null);
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authorization);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 빗썸 API 호출 (주문 가능 정보 조회)
            String url = "https://api.bithumb.com/v1/orders/chance?" + query;
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            // 응답 파싱
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            if (jsonNode.has("status") && jsonNode.has("data")) {
                String status = jsonNode.get("status").asText();
                if (!"0000".equals(status)) {
                    log.error("빗썸 API 에러 응답: {}", response.getBody());
                    throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
                }
                
                return jsonNode.get("data");
            }
            
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
            
        } catch (GeneralSecurityException e) {
            log.error("빗썸 JWT 생성 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        } catch (Exception e) {
            log.error("빗썸 주문 가능 정보 조회 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }
    
    /**
     * market 형식 변환 (KRW-BTC -> BTC_KRW)
     */
    private String convertMarketForBithumb(String market) {
        if (market.contains("-")) {
            String[] parts = market.split("-");
            return parts[1] + "_" + parts[0]; // BTC_KRW
        }
        return market;
    }
    
  
    //주문 가능 여부 검증
    private void validateOrderAvailability(
            String market,
            String side,
            String orderType,
            String price,
            String volume,
            JsonNode orderChanceData
    ) {
        // orderChanceData에서 계좌 잔고 추출
        String balance;
        String currency = extractCurrency(market, side);
        
        if ("KRW".equals(currency)) {
            // KRW 잔고
            balance = orderChanceData.has("available_krw") 
                    ? orderChanceData.get("available_krw").asText() 
                    : "0";
        } else {
            // 코인 잔고
            String availableField = "available_" + currency.toLowerCase();
            balance = orderChanceData.has(availableField) 
                    ? orderChanceData.get(availableField).asText() 
                    : "0";
        }
        
        BigDecimal balanceDecimal = new BigDecimal(balance);
        BigDecimal priceDecimal = new BigDecimal(price);
        BigDecimal volumeDecimal = new BigDecimal(volume);
        
        if ("bid".equals(side)) {
            // 매수: 계좌 잔고 확인
            BigDecimal requiredAmount;
            
            if ("limit".equals(orderType)) {
                // 지정가 매수: price * volume
                requiredAmount = priceDecimal.multiply(volumeDecimal);
            } else if ("price".equals(orderType)) {
                // 시장가 매수: price가 총액
                requiredAmount = priceDecimal;
            } else {
                throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
            }
            
            if (balanceDecimal.compareTo(requiredAmount) < 0) {
                log.warn("계좌 잔고 부족 - 잔고: {}, 필요: {}", balance, requiredAmount);
                throw new InvestException(InvestErrorCode.INSUFFICIENT_BALANCE);
            }
            
        } else if ("ask".equals(side)) {
            // 매도: 코인 보유량 확인
            if (balanceDecimal.compareTo(volumeDecimal) < 0) {
                log.warn("보유 수량 부족 - 보유: {}, 주문: {}", balance, volume);
                throw new InvestException(InvestErrorCode.INSUFFICIENT_COIN_AMOUNT);
            }
        } else {
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }
    
    /**
     * market과 side에서 통화 추출
     * 매수(bid): KRW 필요, 매도(ask): 코인 필요
     */
    private String extractCurrency(String market, String side) {
        if (market.contains("-")) {
            String[] parts = market.split("-");
            if ("bid".equals(side)) {
                return parts[0]; // KRW
            } else {
                return parts[1]; // BTC
            }
        }
        return market;
    }
}