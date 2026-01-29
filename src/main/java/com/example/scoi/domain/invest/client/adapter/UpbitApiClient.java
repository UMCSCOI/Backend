package com.example.scoi.domain.invest.client.adapter;

import com.example.scoi.domain.invest.client.ExchangeApiClient;
import com.example.scoi.domain.invest.client.feign.UpbitFeignClient;
import com.example.scoi.domain.invest.dto.InvestResDTO;
import com.example.scoi.domain.invest.dto.MaxOrderInfoDTO;
import com.example.scoi.domain.invest.exception.InvestException;
import com.example.scoi.domain.invest.exception.code.InvestErrorCode;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.global.client.dto.UpbitResDTO;
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
    public MaxOrderInfoDTO getMaxOrderInfo(String phoneNumber, ExchangeType exchangeType, String coinType, String price) {
        try {
            // coinType을 업비트 형식으로 정규화 (KRW-BTC 형식으로 통일)
            String normalizedCoinType = normalizeCoinType(coinType);
            
            String authorization = jwtApiUtil.createUpBitJwt(phoneNumber, null, null);
            log.info("업비트 최대 주문 정보 조회 API 호출 시작 - phoneNumber: {}, coinType: {} (정규화: {}), price: {}", 
                    phoneNumber, coinType, normalizedCoinType, price);
            
            String responseBody = upbitFeignClient.getAccounts(authorization);
            
            log.info("업비트 최대 주문 정보 조회 API 응답 수신");
            log.debug("업비트 최대 주문 정보 조회 API 응답 본문: {}", responseBody);
            
            return parseMaxOrderInfoResponse(responseBody, normalizedCoinType, price);
            
        } catch (GeneralSecurityException e) {
            log.error("업비트 JWT 생성 실패", e);
            throw new RuntimeException("JWT 생성 실패", e);
        } catch (Exception e) {
            log.error("업비트 최대 주문 정보 조회 API 호출 실패", e);
            throw new RuntimeException("업비트 API 호출 실패: " + e.getMessage(), e);
        }
    }
    
    private MaxOrderInfoDTO parseMaxOrderInfoResponse(String responseBody, String coinType, String price) {
        try {
            List<Map<String, Object>> accounts = objectMapper.readValue(
                responseBody,
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            // coinType이 KRW-BTC 형식이면, 매수 시 KRW 잔액을 조회해야 함
            String currency;
            if (coinType.contains("-")) {
                String[] parts = coinType.split("-");
                // KRW-BTC 형식이면 KRW 잔액 조회 (매수 가능 금액)
                currency = parts[0]; // KRW
            } else {
                currency = coinType; // BTC 등 단일 코인
            }
            
            String balance = "0";
            
            // 먼저 해당 currency로 계좌 찾기
            for (Map<String, Object> account : accounts) {
                String accountCurrency = String.valueOf(account.get("currency"));
                if (currency.equals(accountCurrency)) {
                    // available이 있으면 available 사용 (매수 가능 금액)
                    if (account.containsKey("available") && account.get("available") != null) {
                        balance = String.valueOf(account.get("available"));
                    } else if (account.containsKey("balance") && account.get("balance") != null) {
                        balance = String.valueOf(account.get("balance"));
                    }
                    break;
                }
            }
            
            // price가 있으면 최대 주문 수량 계산 (balance / price)
            String maxQuantity = null;
            if (price != null && !price.isEmpty()) {
                try {
                    java.math.BigDecimal balanceDecimal = new java.math.BigDecimal(balance);
                    java.math.BigDecimal priceDecimal = new java.math.BigDecimal(price);
                    
                    if (priceDecimal.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        maxQuantity = balanceDecimal.divide(priceDecimal, 8, java.math.RoundingMode.DOWN).toPlainString();
                        log.info("업비트 최대 주문 수량 계산 - balance: {}, price: {}, maxQuantity: {}", balance, price, maxQuantity);
                    } else {
                        log.warn("가격이 0 이하입니다. 최대 주문 수량을 계산할 수 없습니다.");
                    }
                } catch (NumberFormatException e) {
                    log.warn("가격 형식이 올바르지 않습니다. 최대 주문 수량을 계산할 수 없습니다. price: {}", price);
                }
            }
            
            log.info("업비트 최대 주문 정보 조회 완료 - coinType: {}, balance: {}, maxQuantity: {}", coinType, balance, maxQuantity);
            
            return MaxOrderInfoDTO.builder()
                    .balance(balance)
                    .maxQuantity(maxQuantity)
                    .build();
                    
        } catch (Exception e) {
            log.error("업비트 최대 주문 정보 조회 API 응답 파싱 실패: {}", responseBody, e);
            throw new RuntimeException("응답 파싱 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * coinType을 업비트 표준 형식(KRW-BTC)으로 정규화
     * - BTC-KRW -> KRW-BTC
     * - KRW-BTC -> KRW-BTC (그대로)
     * - BTC_KRW -> KRW-BTC
     * - KRW만 있는 경우 -> KRW-BTC (기본값)
     */
    private String normalizeCoinType(String coinType) {
        if (coinType == null || coinType.isEmpty()) {
            return "KRW-BTC"; // 기본값
        }
        
        // 이미 KRW-BTC 형식인 경우
        if (coinType.contains("-") && coinType.startsWith("KRW-")) {
            return coinType;
        }
        
        // BTC-KRW 형식인 경우 -> KRW-BTC로 변환
        if (coinType.contains("-")) {
            String[] parts = coinType.split("-");
            if (parts.length == 2) {
                if ("BTC".equals(parts[0]) && "KRW".equals(parts[1])) {
                    return "KRW-BTC";
                }
                // 이미 KRW-XXX 형식이면 그대로 반환
                if ("KRW".equals(parts[0])) {
                    return coinType;
                }
            }
        }
        
        // 언더스코어 형식: BTC_KRW -> KRW-BTC
        if (coinType.contains("_")) {
            String[] parts = coinType.split("_");
            if (parts.length == 2) {
                return parts[1] + "-" + parts[0]; // KRW-BTC
            }
        }
        
        // KRW만 있는 경우
        if ("KRW".equals(coinType)) {
            return "KRW-BTC";
        }
        
        // 변환 불가능한 경우 그대로 반환
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
            log.info("업비트 주문 가능 여부 확인 API 호출 시작 - phoneNumber: {}, market: {}, side: {}", 
                    phoneNumber, market, side);
            
            UpbitResDTO.OrderChance orderChance = getOrderChance(phoneNumber, market);
            validateOrderAvailability(market, side, orderType, price, volume, orderChance);
            
            log.info("업비트 주문 가능 여부 확인 완료 - 주문 가능");
            
        } catch (InvestException e) {
            throw e;
        } catch (Exception e) {
            log.error("업비트 주문 가능 여부 확인 API 호출 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }
    
    private UpbitResDTO.OrderChance getOrderChance(String phoneNumber, String market) {
        try {
            String query = "market=" + market;
            String authorization = jwtApiUtil.createUpBitJwt(phoneNumber, query, null);
            
            String responseBody = upbitFeignClient.getOrderChance(authorization, market);
            
            return objectMapper.readValue(
                responseBody,
                UpbitResDTO.OrderChance.class
            );
            
        } catch (GeneralSecurityException e) {
            log.error("업비트 JWT 생성 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        } catch (feign.FeignException.Unauthorized e) {
            // 401 
            log.error("업비트 API 키 권한 부족", e);
            throw new InvestException(InvestErrorCode.INSUFFICIENT_API_PERMISSION);
        } catch (feign.FeignException.Forbidden e) {
            // 403 
            log.error("업비트 API 키 권한 부족", e);
            throw new InvestException(InvestErrorCode.INSUFFICIENT_API_PERMISSION);
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
            UpbitResDTO.OrderChance orderChance
    ) {
        String balance;
        
        // 잔고 정보 추출
        if ("bid".equals(side)) {
            UpbitResDTO.BidAccount bidAccount = orderChance.bid_account();
            if (bidAccount != null && bidAccount.balance() != null) {
                balance = bidAccount.balance();
            } else {
                log.error("업비트 주문 가능 정보 응답에 bid_account.balance가 없습니다.");
                throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
            }
        } else if ("ask".equals(side)) {
            UpbitResDTO.AskAccount askAccount = orderChance.ask_account();
            if (askAccount != null && askAccount.balance() != null) {
                balance = askAccount.balance();
            } else {
                log.error("업비트 주문 가능 정보 응답에 ask_account.balance가 없습니다.");
                throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
            }
        } else {
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
        
        BigDecimal balanceDecimal = new BigDecimal(balance);
        
        // 주문 가능 여부 검증 (불가능하면 예외 발생)
        String requiredAmountStr = "";
        
        if ("bid".equals(side)) {
            // 매수 주문: price는 필수, volume은 지정가일 때만 필수
            if (price == null || price.isEmpty()) {
                throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
            }
            
            BigDecimal priceDecimal = new BigDecimal(price);
            BigDecimal requiredAmount;
            
            if ("limit".equals(orderType)) {
                // 지정가 매수: price * volume 필요
                if (volume == null || volume.isEmpty()) {
                    throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
                }
                BigDecimal volumeDecimal = new BigDecimal(volume);
                requiredAmount = priceDecimal.multiply(volumeDecimal);
            } else if ("price".equals(orderType)) {
                // 시장가 매수: price만 필요 (총액)
                requiredAmount = priceDecimal;
            } else {
                throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
            }
            
            requiredAmountStr = requiredAmount.toPlainString();
            
            if (balanceDecimal.compareTo(requiredAmount) < 0) {
                // 잔고 부족 시 400 에러 반환
                BigDecimal shortage = requiredAmount.subtract(balanceDecimal);
                log.warn("계좌 잔고 부족 - 잔고: {}, 필요: {}, 부족: {}", balance, requiredAmount, shortage);
                Map<String, String> errorDetails = Map.of(
                    "balance", balance,
                    "requiredAmount", requiredAmountStr,
                    "shortage", shortage.toPlainString()
                );
                throw new InvestException(InvestErrorCode.INSUFFICIENT_BALANCE, errorDetails);
            }
            
        } else if ("ask".equals(side)) {
            // 매도 주문: volume은 필수
            if (volume == null || volume.isEmpty()) {
                throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
            }
            
            BigDecimal volumeDecimal = new BigDecimal(volume);
            requiredAmountStr = volume; // 매도 시 필요한 수량
            
            if (balanceDecimal.compareTo(volumeDecimal) < 0) {
                // 보유 수량 초과 시 400 에러 반환
                BigDecimal shortage = volumeDecimal.subtract(balanceDecimal);
                log.warn("보유 수량 부족 - 보유: {}, 주문: {}, 부족: {}", balance, volume, shortage);
                Map<String, String> errorDetails = Map.of(
                    "balance", balance,
                    "requiredAmount", volume,
                    "shortage", shortage.toPlainString()
                );
                throw new InvestException(InvestErrorCode.INSUFFICIENT_COIN_AMOUNT, errorDetails);
            }
        } else {
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
        
    
    }
}