package com.example.scoi.domain.invest.client.adapter;

import com.example.scoi.domain.invest.client.ExchangeApiClient;
import com.example.scoi.domain.invest.client.feign.UpbitFeignClient;
import com.example.scoi.domain.invest.dto.MaxOrderInfoDTO;
import com.example.scoi.domain.invest.exception.InvestException;
import com.example.scoi.domain.invest.exception.code.InvestErrorCode;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.global.util.JwtApiUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpbitApiClient implements ExchangeApiClient {
    
    private final UpbitFeignClient upbitFeignClient;
    private final JwtApiUtil jwtApiUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public MaxOrderInfoDTO getMaxOrderInfo(String phoneNumber, ExchangeType exchangeType, String coinType) {
        try {
            String authorization = jwtApiUtil.createUpBitJwt(phoneNumber, null, null);
            log.info("업비트 최대 주문 정보 조회 API 호출 시작 - phoneNumber: {}, coinType: {}", phoneNumber, coinType);
            
            String responseBody = upbitFeignClient.getAccounts(authorization);
            
            log.info("업비트 최대 주문 정보 조회 API 응답 수신");
            log.debug("업비트 최대 주문 정보 조회 API 응답 본문: {}", responseBody);
            
            return parseMaxOrderInfoResponse(responseBody, coinType);
            
        } catch (GeneralSecurityException e) {
            log.error("업비트 JWT 생성 실패", e);
            throw new RuntimeException("JWT 생성 실패", e);
        } catch (Exception e) {
            log.error("업비트 최대 주문 정보 조회 API 호출 실패", e);
            throw new RuntimeException("업비트 API 호출 실패: " + e.getMessage(), e);
        }
    }
    
    private MaxOrderInfoDTO parseMaxOrderInfoResponse(String responseBody, String coinType) {
        try {
            List<Map<String, Object>> accounts = objectMapper.readValue(
                responseBody,
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            String currency = coinType.contains("-") ? coinType.split("-")[1] : coinType;
            String balance = "0";
            
            for (Map<String, Object> account : accounts) {
                String accountCurrency = String.valueOf(account.get("currency"));
                if (currency.equals(accountCurrency)) {
                    if (account.containsKey("available")) {
                        balance = String.valueOf(account.get("available"));
                    } else if (account.containsKey("balance")) {
                        balance = String.valueOf(account.get("balance"));
                    }
                    break;
                }
            }
            
            if ("KRW".equals(currency)) {
                for (Map<String, Object> account : accounts) {
                    if ("KRW".equals(account.get("currency"))) {
                        balance = String.valueOf(account.get("balance"));
                        break;
                    }
                }
            }
            
            log.info("업비트 최대 주문 정보 조회 완료 - coinType: {}, balance: {}", coinType, balance);
            
            return MaxOrderInfoDTO.builder()
                    .balance(balance)
                    .build();
                    
        } catch (Exception e) {
            log.error("업비트 최대 주문 정보 조회 API 응답 파싱 실패: {}", responseBody, e);
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
            log.info("업비트 주문 가능 여부 확인 API 호출 시작 - phoneNumber: {}, market: {}, side: {}", 
                    phoneNumber, market, side);
            
            Map<String, Object> orderChance = getOrderChance(phoneNumber, market);
            validateOrderAvailability(market, side, orderType, price, volume, orderChance);
            
            log.info("업비트 주문 가능 여부 확인 완료 - 주문 가능");
            
        } catch (InvestException e) {
            throw e;
        } catch (Exception e) {
            log.error("업비트 주문 가능 여부 확인 API 호출 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }
    
    private Map<String, Object> getOrderChance(String phoneNumber, String market) {
        try {
            String query = "market=" + market;
            String authorization = jwtApiUtil.createUpBitJwt(phoneNumber, query, null);
            
            String responseBody = upbitFeignClient.getOrderChance(authorization, market);
            
            return objectMapper.readValue(
                responseBody,
                new TypeReference<Map<String, Object>>() {}
            );
            
        } catch (GeneralSecurityException e) {
            log.error("업비트 JWT 생성 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        } catch (Exception e) {
            log.error("업비트 주문 가능 정보 조회 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }
  
    private void validateOrderAvailability(
            String market,
            String side,
            String orderType,
            String price,
            String volume,
            Map<String, Object> orderChance
    ) {
        String balance;
        if ("bid".equals(side)) {
            Map<String, Object> bidAccount = (Map<String, Object>) orderChance.get("bid_account");
            if (bidAccount != null && bidAccount.containsKey("balance")) {
                balance = String.valueOf(bidAccount.get("balance"));
            } else {
                log.error("업비트 주문 가능 정보 응답에 bid_account.balance가 없습니다.");
                throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
            }
        } else if ("ask".equals(side)) {
            Map<String, Object> askAccount = (Map<String, Object>) orderChance.get("ask_account");
            if (askAccount != null && askAccount.containsKey("balance")) {
                balance = String.valueOf(askAccount.get("balance"));
            } else {
                log.error("업비트 주문 가능 정보 응답에 ask_account.balance가 없습니다.");
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