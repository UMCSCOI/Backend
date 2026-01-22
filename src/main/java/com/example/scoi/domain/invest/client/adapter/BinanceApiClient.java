package com.example.scoi.domain.invest.client.adapter;

import com.example.scoi.domain.invest.client.ExchangeApiClient;
import com.example.scoi.domain.invest.client.feign.BinanceFeignClient;
import com.example.scoi.domain.invest.dto.MaxOrderInfoDTO;
import com.example.scoi.domain.invest.exception.InvestException;
import com.example.scoi.domain.invest.exception.code.InvestErrorCode;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.global.util.JwtApiUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.GeneralSecurityException;

@Component
@RequiredArgsConstructor
@Slf4j
public class BinanceApiClient implements ExchangeApiClient {
    
    private final BinanceFeignClient binanceFeignClient;
    private final JwtApiUtil jwtApiUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public MaxOrderInfoDTO getMaxOrderInfo(String phoneNumber, ExchangeType exchangeType, String coinType) {
        try {
            log.info("바이낸스 최대 주문 정보 조회 API 호출 시작 - phoneNumber: {}, coinType: {}", phoneNumber, coinType);
            
            // 1. HMAC-SHA256 인증 정보 생성
            JwtApiUtil.BinanceApiAuthInfo authInfo = jwtApiUtil.createBinanceAuth(phoneNumber);
            
            // 2. query string에서 timestamp와 signature 추출
            String[] queryParams = authInfo.getQueryString().split("&");
            String timestamp = "";
            String signature = "";
            for (String param : queryParams) {
                if (param.startsWith("timestamp=")) {
                    timestamp = param.substring("timestamp=".length());
                } else if (param.startsWith("signature=")) {
                    signature = param.substring("signature=".length());
                }
            }
            
            // 3. Feign Client를 통한 API 호출
            String responseBody = binanceFeignClient.getAccount(authInfo.getApiKey(), timestamp, signature);
            
            log.info("바이낸스 최대 주문 정보 조회 API 응답 수신");
            log.debug("바이낸스 최대 주문 정보 조회 API 응답 본문: {}", responseBody);
            
            // 3. 응답 파싱 및 변환
            return parseMaxOrderInfoResponse(responseBody, coinType);
            
        } catch (GeneralSecurityException e) {
            log.error("바이낸스 HMAC-SHA256 서명 생성 실패", e);
            throw new RuntimeException("인증 정보 생성 실패", e);
        } catch (Exception e) {
            log.error("바이낸스 최대 주문 정보 조회 API 호출 실패", e);
            throw new RuntimeException("바이낸스 API 호출 실패: " + e.getMessage(), e);
        }
    }
    
    private MaxOrderInfoDTO parseMaxOrderInfoResponse(String responseBody, String coinType) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            String balance = "0";
            
            // 바이낸스 API 응답 형식: balances 배열에서 해당 코인 찾기
            if (jsonNode.has("balances")) {
                JsonNode balances = jsonNode.get("balances");
                
                // coinType 형식: KRW-BTC -> BTC
                String currency = coinType.contains("-") ? coinType.split("-")[1] : coinType;
                
                for (JsonNode balanceNode : balances) {
                    String asset = balanceNode.get("asset").asText();
                    if (currency.equals(asset)) {
                        // free 필드가 있으면 사용, 없으면 사용 가능한 잔고는 0
                        if (balanceNode.has("free")) {
                            balance = balanceNode.get("free").asText();
                        }
                        break;
                    }
                }
                
                // KRW인 경우 USDT나 다른 스테이블 코인으로 변환 필요할 수 있음
                // 바이낸스는 KRW를 직접 지원하지 않을 수 있음
                if ("KRW".equals(currency)) {
                    // USDT나 다른 스테이블 코인으로 처리
                    for (JsonNode balanceNode : balances) {
                        String asset = balanceNode.get("asset").asText();
                        if ("USDT".equals(asset) || "BUSD".equals(asset)) {
                            if (balanceNode.has("free")) {
                                balance = balanceNode.get("free").asText();
                            }
                            break;
                        }
                    }
                }
            }
            
            return MaxOrderInfoDTO.builder()
                    .balance(balance)
                    .build();
                    
        } catch (Exception e) {
            log.error("바이낸스 최대 주문 정보 조회 API 응답 파싱 실패: {}", responseBody, e);
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
            log.info("바이낸스 주문 가능 여부 확인 API 호출 시작 - phoneNumber: {}, market: {}, side: {}", 
                    phoneNumber, market, side);
            
            // 1. 주문 가능 정보 조회
            JsonNode orderInfo = getOrderInfo(phoneNumber, market);
            
            // 2. 주문 가능 여부 검증
            validateOrderAvailability(market, side, orderType, price, volume, orderInfo);
            
            log.info("바이낸스 주문 가능 여부 확인 완료 - 주문 가능");
            
        } catch (InvestException e) {
            throw e;
        } catch (Exception e) {
            log.error("바이낸스 주문 가능 여부 확인 API 호출 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }
    
    private JsonNode getOrderInfo(String phoneNumber, String market) {
        try {
            // 1. HMAC-SHA256 인증 정보 생성
            JwtApiUtil.BinanceApiAuthInfo authInfo = jwtApiUtil.createBinanceAuth(phoneNumber);
            
            // 2. market 형식 변환 (KRW-BTC -> BTCUSDT 등)
            String symbol = convertMarketForBinance(market);
            
            // 3. query string에서 timestamp와 signature 추출
            String[] queryParams = authInfo.getQueryString().split("&");
            String timestamp = "";
            String signature = "";
            for (String param : queryParams) {
                if (param.startsWith("timestamp=")) {
                    timestamp = param.substring("timestamp=".length());
                } else if (param.startsWith("signature=")) {
                    signature = param.substring("signature=".length());
                }
            }
            
            // 4. Feign Client를 통한 API 호출
            String responseBody = binanceFeignClient.getOrderInfo(authInfo.getApiKey(), symbol, timestamp, signature);
            
            log.info("바이낸스 API 응답 본문: {}", responseBody);
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            return jsonNode;
            
        } catch (GeneralSecurityException e) {
            log.error("바이낸스 HMAC-SHA256 서명 생성 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        } catch (Exception e) {
            log.error("바이낸스 주문 가능 정보 조회 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }
    
    /**
     * market 형식 변환 (KRW-BTC -> BTCUSDT)
     * 바이낸스는 USDT, BUSD 등의 스테이블 코인을 사용
     */
    private String convertMarketForBinance(String market) {
        // KRW-BTC -> BTCUSDT
        if (market.contains("-")) {
            String[] parts = market.split("-");
            String baseCurrency = parts[1]; // BTC
            // 바이낸스는 보통 USDT를 사용
            return baseCurrency + "USDT";
        }
        return market;
    }
  
    private void validateOrderAvailability(
            String market,
            String side,
            String orderType,
            String price,
            String volume,
            JsonNode orderInfo
    ) {
        // 바이낸스 API 응답 형식에 맞게 balance 추출
        // 실제 바이낸스 API 문서에 따라 수정 필요
        String balance = "0";
        
        // 바이낸스는 계좌 정보에서 잔고를 확인해야 함
        // 여기서는 간단히 예시만 제공 (실제 API 응답 형식에 맞게 수정 필요)
        if (orderInfo.has("balance")) {
            balance = orderInfo.get("balance").asText();
        } else {
            log.error("바이낸스 응답에 balance가 없습니다.");
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
        
        BigDecimal balanceDecimal = new BigDecimal(balance);
        BigDecimal priceDecimal = new BigDecimal(price);
        BigDecimal volumeDecimal = new BigDecimal(volume);
        
        if ("bid".equals(side)) {
            // 매수: 계좌 잔고 확인
            BigDecimal requiredAmount;
            
            if ("limit".equals(orderType)) {
                requiredAmount = priceDecimal.multiply(volumeDecimal);
            } else if ("market".equals(orderType)) {
                // 시장가 매수: 가격 정보 필요
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
}
