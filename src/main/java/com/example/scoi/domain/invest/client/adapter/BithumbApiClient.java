package com.example.scoi.domain.invest.client.adapter;

import com.example.scoi.domain.invest.client.ExchangeApiClient;
import com.example.scoi.domain.invest.client.feign.BithumbFeignClient;
import com.example.scoi.domain.invest.dto.InvestResDTO;
import com.example.scoi.domain.invest.dto.MaxOrderInfoDTO;
import com.example.scoi.domain.invest.exception.InvestException;
import com.example.scoi.domain.invest.exception.code.InvestErrorCode;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.global.client.dto.BithumbResDTO;
import com.example.scoi.global.util.JwtApiUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class BithumbApiClient implements ExchangeApiClient {
    
    private final BithumbFeignClient bithumbFeignClient;
    private final JwtApiUtil jwtApiUtil;
    
    @Override
    public MaxOrderInfoDTO getMaxOrderInfo(String phoneNumber, ExchangeType exchangeType, String coinType, String price) {
        try {
            // coinType을 마켓 형식으로
            String market = convertCoinTypeToMarket(coinType);
            
            log.info("빗썸 최대 주문 정보 조회 API 호출 시작 - phoneNumber: {}, coinType: {} (정규화: {}), price: {}", 
                    phoneNumber, coinType, market, price);
            
            // 주문 가능 정보 조회 
            BithumbResDTO.OrderChance orderChance = getOrderChance(phoneNumber, market);
            
            log.info("빗썸 최대 주문 정보 조회 API 응답 수신 완료");
            
            // 응답 파싱 및 변환 (bid_account.balance 사용 - 매수 가능 잔고)
            return parseMaxOrderInfoFromOrderChance(orderChance, price);
            
        } catch (Exception e) {
            log.error("빗썸 최대 주문 정보 조회 API 호출 실패", e);
            throw new RuntimeException("빗썸 API 호출 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * /v1/orders/chance API 응답에서 최대 주문 정보 파싱
     * price가 있으면 balance / price로 최대 주문 수량(maxQuantity)
     */
    private MaxOrderInfoDTO parseMaxOrderInfoFromOrderChance(BithumbResDTO.OrderChance orderChance, String price) {
        try {
            String balance = "0";
            
            // bid_account와 ask_account 정보 로깅
            if (orderChance.bid_account() != null) {
                BithumbResDTO.BidAccount bidAccount = orderChance.bid_account();
                log.info("빗썸 bid_account 정보 - currency: {}, balance: {}, locked: {}, avg_buy_price: {}", 
                        bidAccount.currency(), bidAccount.balance(), bidAccount.locked(), bidAccount.avg_buy_price());
            }
            if (orderChance.ask_account() != null) {
                BithumbResDTO.AskAccount askAccount = orderChance.ask_account();
                log.info("빗썸 ask_account 정보 - currency: {}, balance: {}, locked: {}, avg_buy_price: {}", 
                        askAccount.currency(), askAccount.balance(), askAccount.locked(), askAccount.avg_buy_price());
            }
            
            // bid_account에서 balance 추출  
            if (orderChance.bid_account() != null) {
                BithumbResDTO.BidAccount bidAccount = orderChance.bid_account();
                if (bidAccount.balance() != null && !bidAccount.balance().isEmpty()) {
                    balance = bidAccount.balance();
                    log.info("빗썸 bid_account balance 사용: {}", balance);
                } else {
                    log.warn("빗썸 주문 가능 정보 응답에 bid_account.balance가 없습니다.");
                }
            } else {
                log.warn("빗썸 주문 가능 정보 응답에 bid_account가 없습니다.");
            }
            
            // price가 있으면 최대 주문 수량 계산 (balance / price)
            String maxQuantity = null;
            if (price != null && !price.isEmpty()) {
                try {
                    java.math.BigDecimal balanceDecimal = new java.math.BigDecimal(balance);
                    java.math.BigDecimal priceDecimal = new java.math.BigDecimal(price);
                    
                    if (priceDecimal.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        maxQuantity = balanceDecimal.divide(priceDecimal, 8, java.math.RoundingMode.DOWN).toPlainString();
                        log.info("빗썸 최대 주문 수량 계산 - balance: {}, price: {}, maxQuantity: {}", balance, price, maxQuantity);
                    } else {
                        log.warn("가격이 0 이하입니다. 최대 주문 수량을 계산할 수 없습니다.");
                    }
                } catch (NumberFormatException e) {
                    log.warn("가격 형식이 올바르지 않습니다. 최대 주문 수량을 계산할 수 없습니다. price: {}", price);
                }
            }
            
            log.info("빗썸 최대 주문 정보 조회 완료 - balance: {}, maxQuantity: {}", balance, maxQuantity);
            
            return MaxOrderInfoDTO.builder()
                    .balance(balance)
                    .maxQuantity(maxQuantity)
                    .build();
                    
        } catch (Exception e) {
            log.error("빗썸 최대 주문 정보 조회 API 응답 파싱 실패", e);
            throw new RuntimeException("응답 파싱 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * coinType을 빗썸 표준 형식(KRW-BTC)으로 정규화
     * - BTC-KRW -> KRW-BTC
     * - KRW-BTC -> KRW-BTC (그대로)
     * - BTC_KRW -> KRW-BTC
     * - KRW만 있는 경우 -> KRW-BTC (기본값)
     */
    private String convertCoinTypeToMarket(String coinType) {
        if (coinType == null || coinType.isEmpty()) {
            return "KRW-BTC"; // 기본값
        }
        
        // 이미 KRW-BTC 형식인 경우
        if (coinType.contains("-") && coinType.startsWith("KRW-")) {
            return coinType;
        }
        
        // BTC-KRW -> KRW-BTC
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
        
        //  BTC_KRW -> KRW-BTC
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
            log.info("빗썸 주문 가능 여부 확인 API 호출 시작 - phoneNumber: {}, market: {}, side: {}", 
                    phoneNumber, market, side);
            
            // 주문 가능 정보 조회 
            BithumbResDTO.OrderChance orderChance = getOrderChance(phoneNumber, market);
            
            // 주문 가능 여부 검증 
            validateOrderAvailability(market, side, orderType, price, volume, orderChance);
            
            log.info("빗썸 주문 가능 여부 확인 완료 - 주문 가능");
            
        } catch (InvestException e) {
            throw e;
        } catch (Exception e) {
            log.error("빗썸 주문 가능 여부 확인 API 호출 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }
   
    private BithumbResDTO.OrderChance getOrderChance(String phoneNumber, String market) {
        try {
            String convertedMarket = convertMarketForBithumb(market);
            String query = "market=" + convertedMarket;
            log.info("빗썸 주문 가능 정보 조회 - 원본 마켓: {}, 변환된 마켓: {}, query: {}", market, convertedMarket, query);
            //빗썸용 JWT 생성
            String authorization = jwtApiUtil.createBithumbJwt(phoneNumber, query, null);
            
            // Feign Client를 통한 API 호출
            // Feign Client가 DTO로 변환
            log.info("빗썸 API 호출 시작 - market: {}", convertedMarket);
            BithumbResDTO.OrderChance orderChance = bithumbFeignClient.getOrderChance(authorization, convertedMarket);
            
            log.info("빗썸 API 응답 수신 완료");
            
            if (orderChance.bid_account() != null || orderChance.ask_account() != null) {
                return orderChance;
            }
            
            log.error("빗썸 API 응답 형식이 예상과 다름. 응답 본문: {}", orderChance);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
            
        } catch (GeneralSecurityException e) {
            log.error("빗썸 JWT 생성 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        } catch (feign.FeignException.Unauthorized e) {
            // 401 Unauthorized: API 키 권한 부족
            log.error("빗썸 API 키 권한 부족", e);
            throw new InvestException(InvestErrorCode.INSUFFICIENT_API_PERMISSION);
        } catch (Exception e) {
            log.error("빗썸 주문 가능 정보 조회 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }
    
    // 업비트 마켓 형식(KRW-BTC)을 빗썸 마켓 형식으로 변환

    private String convertMarketForBithumb(String market) {
        // 빗썸 API는 업비트와 동일한 형식(KRW-BTC)을 사용하는 것으로 추정
        // 이미 KRW-BTC 형식이면 그대로 사용
        if (market.contains("-") && market.startsWith("KRW-")) {
            return market;
        }
        
        // 업비트 형식: KRW-BTC -> 그대로 사용 
        if (market.contains("-")) {
            String[] parts = market.split("-");
            if (parts.length == 2 && parts[0].equals("KRW")) {
                // KRW-BTC -> KRW-BTC
                return market;
            }
        }
        
        // KRW만 들어온 경우: KRW -> KRW-BTC
        if ("KRW".equals(market)) {
            log.warn("마켓이 'KRW'만 입력되었습니다. 기본값 'KRW-BTC'로 변환합니다.");
            return "KRW-BTC";
        }
        
        // 언더스코어 형식이 들어온 경우: BTC_KRW -> KRW-BTC
        if (market.contains("_")) {
            String[] parts = market.split("_");
            if (parts.length == 2) {
                // BTC_KRW -> KRW-BTC
                return parts[1] + "-" + parts[0];
            }
        }
        
        // 변환 불가능한 경우 그대로 반환
        return market;
    }
  
    private void validateOrderAvailability(
            String market,
            String side,
            String orderType,
            String price,
            String volume,
            BithumbResDTO.OrderChance orderChance
    ) {
        String balance;
        
        // 잔고 정보 추출
        if ("bid".equals(side)) {
            BithumbResDTO.BidAccount bidAccount = orderChance.bid_account();
            if (bidAccount != null && bidAccount.balance() != null) {
                balance = bidAccount.balance();
            } else {
                log.error("빗썸 응답에 bid_account.balance가 없습니다.");
                throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
            }
        } else if ("ask".equals(side)) {
            BithumbResDTO.AskAccount askAccount = orderChance.ask_account();
            if (askAccount != null && askAccount.balance() != null) {
                balance = askAccount.balance();
            } else {
                log.error("빗썸 응답에 ask_account.balance가 없습니다.");
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