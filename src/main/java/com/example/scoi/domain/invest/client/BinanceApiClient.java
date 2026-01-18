package com.example.scoi.domain.invest.client;

import com.example.scoi.domain.invest.dto.InvestResDTO;
import com.example.scoi.domain.invest.exception.InvestException;
import com.example.scoi.domain.invest.exception.code.InvestErrorCode;
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

import java.math.BigDecimal;
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
    public InvestResDTO.MaxOrderInfoDTO getMaxOrderInfo(String phoneNumber, ExchangeType exchangeType, String coinType) {
        try {
            log.info("바이낸스 최대 주문 정보 조회 API 호출 시작 - phoneNumber: {}, coinType: {}", phoneNumber, coinType);
            
            // HMAC-SHA256 서명 생성 (JwtApiUtil 사용)
            JwtApiUtil.BinanceApiAuthInfo authInfo = jwtApiUtil.createBinanceAuth(phoneNumber);
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-MBX-APIKEY", authInfo.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 바이낸스 API 호출 (계정 잔고 조회) -문서 다시 확인 
            String url = "https://api.binance.com/api/v3/account?" + authInfo.getQueryString();
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            log.info("바이낸스 최대 주문 정보 조회 API 응답 상태: {}", response.getStatusCode());
            log.debug("바이낸스 최대 주문 정보 조회 API 응답 본문: {}", response.getBody());
            
            // 응답 파싱 및 변환
            return parseMaxOrderInfoResponse(response.getBody(), coinType);
            
        } catch (GeneralSecurityException e) {
            log.error("바이낸스 HMAC-SHA256 서명 생성 실패", e);
            throw new RuntimeException("HMAC-SHA256 서명 생성 실패", e);
        } catch (Exception e) {
            log.error("바이낸스 최대 주문 정보 조회 API 호출 실패", e);
            throw new RuntimeException("바이낸스 API 호출 실패: " + e.getMessage(), e);
        }
    }
    
    private InvestResDTO.MaxOrderInfoDTO parseMaxOrderInfoResponse(String responseBody, String coinType) {
        try {
            // 바이낸스 API 응답 형식에 맞게 파싱
            
            Map<String, Object> account = objectMapper.readValue(
                responseBody,
                new TypeReference<Map<String, Object>>() {}
            );
            
            // coinType 변환 (KRW-BTC -> BTCUSDT 등)
            String asset = convertCoinType(coinType);
            
            // balances 배열에서 해당 asset 찾기
            List<Map<String, Object>> balances = (List<Map<String, Object>>) account.get("balances");
            
            String balance = "0";
            if (balances != null) {
                for (Map<String, Object> balanceInfo : balances) {
                    String balanceAsset = String.valueOf(balanceInfo.get("asset"));
                    if (asset.equals(balanceAsset)) {
                        // free 필드가 사용 가능한 잔액
                        balance = String.valueOf(balanceInfo.get("free"));
                        break;
                    }
                }
            }
            
            log.info("바이낸스 최대 주문 정보 조회 완료 - coinType: {}, balance: {}", coinType, balance);
            
            return InvestResDTO.MaxOrderInfoDTO.builder()
                    .balance(balance)
                    .build();
                    
        } catch (Exception e) {
            log.error("바이낸스 최대 주문 정보 조회 API 응답 파싱 실패: {}", responseBody, e);
            throw new RuntimeException("응답 파싱 실패: " + e.getMessage(), e);
        }
    }
    
   
    //coinType 변환 (업비트 형식 -> 바이낸스 형식)
    private String convertCoinType(String coinType) {
        // 업비트: KRW-BTC, 바이낸스: BTCUSDT
        if (coinType.startsWith("KRW-")) {
            String coin = coinType.substring(4); // KRW- 제거
            // 바이낸스는 보통 USDT 페어 사용
            return coin + "USDT";
        }
        return coinType;
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
            log.info("바이낸스 주문 가능 여부 확인 API 호출 시작 - phoneNumber: {}, market: {}, side: {}", 
                    phoneNumber, market, side);
            
            // 1. 계좌 잔고 조회
            String balance = getAccountBalance(phoneNumber, market, side);
            
            // 2. 주문 가능 여부 검증
            validateOrderAvailability(market, side, orderType, price, volume, balance);
            
            log.info("바이낸스 주문 가능 여부 확인 완료 - 주문 가능");
            
        } catch (InvestException e) {
            throw e;
        } catch (Exception e) {
            log.error("바이낸스 주문 가능 여부 확인 API 호출 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }
   
    //계좌 잔고 조회
    private String getAccountBalance(String phoneNumber, String market, String side) {
        try {
            // HMAC-SHA256 서명 생성
            JwtApiUtil.BinanceApiAuthInfo authInfo = jwtApiUtil.createBinanceAuth(phoneNumber);
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-MBX-APIKEY", authInfo.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 바이낸스 API 호출 (계정 잔고 조회) - 문서 확인 필요 
            String url = "https://api.binance.com/api/v3/account?" + authInfo.getQueryString();
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            // 응답 파싱
            Map<String, Object> account = objectMapper.readValue(
                response.getBody(),
                new TypeReference<Map<String, Object>>() {}
            );
            
            // market과 side에 따라 필요한 asset 추출
            String asset = extractAsset(market, side);
            
            // balances 배열에서 해당 asset 찾기
            List<Map<String, Object>> balances = (List<Map<String, Object>>) account.get("balances");
            
            if (balances != null) {
                for (Map<String, Object> balanceInfo : balances) {
                    String balanceAsset = String.valueOf(balanceInfo.get("asset"));
                    if (asset.equals(balanceAsset)) {
                        // free 필드가 사용 가능한 잔액
                        return String.valueOf(balanceInfo.get("free"));
                    }
                }
            }
            
            return "0";
            
        } catch (GeneralSecurityException e) {
            log.error("바이낸스 HMAC-SHA256 서명 생성 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        } catch (Exception e) {
            log.error("바이낸스 계좌 잔고 조회 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }
  
    //주문 가능 여부 검증
    private void validateOrderAvailability(
            String market,
            String side,
            String orderType,
            String price,
            String volume,
            String balance
    ) {
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
     * market과 side에서 asset 추출
     */
    private String extractAsset(String market, String side) {
        if (market.contains("-")) {
            String[] parts = market.split("-");
            if ("bid".equals(side)) {
                // 매수: KRW 필요하지만 바이낸스는 USDT/BUSD 등을 사용
                // 실제로는 market이 KRW-BTC 형태가 아닐 수 있으므로 주의
                return "USDT"; // 또는 필요한 base asset
            } else {
                return parts[1]; // BTC
            }
        }
        return market;
    }
}