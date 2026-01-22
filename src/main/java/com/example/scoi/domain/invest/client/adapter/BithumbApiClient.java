package com.example.scoi.domain.invest.client.adapter;

import com.example.scoi.domain.invest.client.ExchangeApiClient;
import com.example.scoi.domain.invest.client.feign.BithumbFeignClient;
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
public class BithumbApiClient implements ExchangeApiClient {
    
    private final BithumbFeignClient bithumbFeignClient;
    private final JwtApiUtil jwtApiUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public MaxOrderInfoDTO getMaxOrderInfo(String phoneNumber, ExchangeType exchangeType, String coinType) {
        try {
            log.info("빗썸 최대 주문 정보 조회 API 호출 시작 - phoneNumber: {}, coinType: {}", phoneNumber, coinType);
            
            // 1. coinType을 마켓 형식으로 변환 (예: KRW-BTC -> KRW-BTC)
            String market = convertCoinTypeToMarket(coinType);
            
            // 2. 주문 가능 정보 조회 (/v1/orders/chance)
            // getOrderChance는 내부에서 GeneralSecurityException을 처리하므로 여기서는 발생하지 않음
            JsonNode orderChance = getOrderChance(phoneNumber, market);
            
            log.info("빗썸 최대 주문 정보 조회 API 응답 수신");
            log.debug("빗썸 최대 주문 정보 조회 API 응답 본문: {}", orderChance.toPrettyString());
            
            // 3. 응답 파싱 및 변환 (bid_account.balance 사용 - 매수 가능 잔고)
            return parseMaxOrderInfoFromOrderChance(orderChance);
            
        } catch (Exception e) {
            log.error("빗썸 최대 주문 정보 조회 API 호출 실패", e);
            throw new RuntimeException("빗썸 API 호출 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * /v1/orders/chance API 응답에서 최대 주문 정보 파싱
     * bid_account.balance를 반환 (매수 가능 잔고)
     */
    private MaxOrderInfoDTO parseMaxOrderInfoFromOrderChance(JsonNode orderChance) {
        try {
            String balance = "0";
            
            // bid_account에서 balance 추출 (매수 가능 잔고)
            if (orderChance.has("bid_account")) {
                JsonNode bidAccount = orderChance.get("bid_account");
                if (bidAccount.has("balance")) {
                    balance = bidAccount.get("balance").asText();
                } else if (bidAccount.has("available")) {
                    balance = bidAccount.get("available").asText();
                } else {
                    log.warn("빗썸 주문 가능 정보 응답에 bid_account.balance가 없습니다. bid_account: {}", bidAccount.toPrettyString());
                }
            } else {
                log.warn("빗썸 주문 가능 정보 응답에 bid_account가 없습니다. 전체 응답: {}", orderChance.toPrettyString());
            }
            
            log.info("빗썸 최대 주문 정보 조회 완료 - balance: {}", balance);
            
            return MaxOrderInfoDTO.builder()
                    .balance(balance)
                    .build();
                    
        } catch (Exception e) {
            log.error("빗썸 최대 주문 정보 조회 API 응답 파싱 실패: {}", orderChance.toPrettyString(), e);
            throw new RuntimeException("응답 파싱 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * coinType을 마켓 형식으로 변환
     * 예: KRW-BTC -> KRW-BTC, KRW -> KRW-BTC (기본값)
     */
    private String convertCoinTypeToMarket(String coinType) {
        // coinType이 이미 마켓 형식인 경우 (예: KRW-BTC)
        if (coinType.contains("-")) {
            return coinType;
        }
        
        // coinType이 KRW만 있는 경우 기본 마켓으로 변환
        if ("KRW".equals(coinType)) {
            return "KRW-BTC"; // 기본 마켓 (필요시 변경 가능)
        }
        
        // 그 외의 경우 coinType을 그대로 사용
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
   
    private JsonNode getOrderChance(String phoneNumber, String market) {
        try {
            String convertedMarket = convertMarketForBithumb(market);
            String query = "market=" + convertedMarket;
            String authorization = jwtApiUtil.createBithumbJwt(phoneNumber, query, null);
            
            // Feign Client를 통한 API 호출
            String responseBody = bithumbFeignClient.getOrderChance(authorization, convertedMarket);
            
            log.info("빗썸 API 응답 본문: {}", responseBody);
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            if (jsonNode.has("bid_account") || jsonNode.has("ask_account")) {
                return jsonNode;
            }
            
            log.error("빗썸 API 응답 형식이 예상과 다름. 응답 본문: {}", responseBody);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
            
        } catch (GeneralSecurityException e) {
            log.error("빗썸 JWT 생성 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        } catch (Exception e) {
            log.error("빗썸 주문 가능 정보 조회 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }
    
    private String convertMarketForBithumb(String market) {
        return market; // 필요시 변환 로직 추가
    }
  
    private void validateOrderAvailability(
            String market,
            String side,
            String orderType,
            String price,
            String volume,
            JsonNode orderChanceData
    ) {
        String balance;
        
        if ("bid".equals(side)) {
            JsonNode bidAccount = orderChanceData.get("bid_account");
            if (bidAccount != null && bidAccount.has("balance")) {
                balance = bidAccount.get("balance").asText();
            } else {
                log.error("빗썸 응답에 bid_account.balance가 없습니다.");
                throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
            }
        } else if ("ask".equals(side)) {
            JsonNode askAccount = orderChanceData.get("ask_account");
            if (askAccount != null && askAccount.has("balance")) {
                balance = askAccount.get("balance").asText();
            } else {
                log.error("빗썸 응답에 ask_account.balance가 없습니다.");
                throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
            }
        } else {
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
        
        BigDecimal balanceDecimal = new BigDecimal(balance);
        BigDecimal priceDecimal = new BigDecimal(price);
        BigDecimal volumeDecimal = new BigDecimal(volume);
        
        if ("bid".equals(side)) {
            BigDecimal requiredAmount;
            
            if ("limit".equals(orderType)) {
                requiredAmount = priceDecimal.multiply(volumeDecimal);
            } else if ("price".equals(orderType)) {
                requiredAmount = priceDecimal;
            } else {
                throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
            }
            
            if (balanceDecimal.compareTo(requiredAmount) < 0) {
                log.warn("계좌 잔고 부족 - 잔고: {}, 필요: {}", balance, requiredAmount);
                throw new InvestException(InvestErrorCode.INSUFFICIENT_BALANCE);
            }
            
        } else if ("ask".equals(side)) {
            if (balanceDecimal.compareTo(volumeDecimal) < 0) {
                log.warn("보유 수량 부족 - 보유: {}, 주문: {}", balance, volume);
                throw new InvestException(InvestErrorCode.INSUFFICIENT_COIN_AMOUNT);
            }
        } else {
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }
}