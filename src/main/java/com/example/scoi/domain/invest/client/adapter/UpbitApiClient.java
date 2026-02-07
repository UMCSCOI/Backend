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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpbitApiClient implements ExchangeApiClient {
    
    private final UpbitFeignClient upbitFeignClient;
    private final JwtApiUtil jwtApiUtil; 
    
    @Override
    public MaxOrderInfoDTO getMaxOrderInfo(String phoneNumber, ExchangeType exchangeType, String coinType, String price) {
        try {
            // coinType을 업비트 형식으로 정규화 (KRW-BTC 형식으로 통일)
            String normalizedCoinType = normalizeCoinType(coinType);
            
            String authorization = jwtApiUtil.createUpBitJwt(phoneNumber, null, null);
            log.info("업비트 최대 주문 정보 조회 API 호출 시작 - phoneNumber: {}, coinType: {} (정규화: {}), price: {}", 
                    phoneNumber, coinType, normalizedCoinType, price);
            
            // Feign Client가 자동으로 List<Account>로 변환해줌 (ObjectMapper 불필요!)
            List<UpbitResDTO.Account> accounts = upbitFeignClient.getAccounts(authorization);
            
            log.info("업비트 최대 주문 정보 조회 API 응답 수신 - 계좌 개수: {}", accounts.size());
            // 디버깅: 실제 응답 값 확인
            for (UpbitResDTO.Account account : accounts) {
                log.info("계좌 정보 - currency: {}, balance: {}, locked: {}, available: {}", 
                        account.currency(), account.balance(), account.locked(), account.available());
            }
            
            return parseMaxOrderInfoResponse(accounts, normalizedCoinType, price);
            
        } catch (GeneralSecurityException e) {
            log.error("업비트 JWT 생성 실패", e);
            throw new RuntimeException("JWT 생성 실패", e);
        } catch (Exception e) {
            log.error("업비트 최대 주문 정보 조회 API 호출 실패", e);
            throw new RuntimeException("업비트 API 호출 실패: " + e.getMessage(), e);
        }
    }
    
    private MaxOrderInfoDTO parseMaxOrderInfoResponse(List<UpbitResDTO.Account> accounts, String coinType, String price) {
        try {
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
            for (UpbitResDTO.Account account : accounts) {
                if (currency.equals(account.currency())) {
                    log.info("해당 currency 계좌 발견 - currency: {}, balance: {}, locked: {}, available: {}", 
                            account.currency(), account.balance(), account.locked(), account.available());
                    
                    // available이 있으면 available 사용 (매수 가능 금액)
                    // 주의: 업비트 API /v1/accounts는 available 필드를 제공하지 않음 (공식 문서 확인)
                    // 따라서 available이 null이면 balance - locked로 계산해야 함
                    if (account.available() != null && !account.available().isEmpty()) {
                        balance = account.available();
                        log.info("available 사용: {}", balance);
                    } else if (account.balance() != null && !account.balance().isEmpty()) {
                        // available이 null이면 balance - locked로 계산
                        try {
                            BigDecimal balanceDecimal = new BigDecimal(account.balance());
                            BigDecimal lockedDecimal = account.locked() != null && !account.locked().isEmpty() 
                                    ? new BigDecimal(account.locked()) 
                                    : BigDecimal.ZERO;
                            BigDecimal availableDecimal = balanceDecimal.subtract(lockedDecimal);
                            balance = availableDecimal.toPlainString();
                            log.info("available 계산 사용 - balance: {}, locked: {}, available: {}", 
                                    account.balance(), account.locked(), balance);
                        } catch (NumberFormatException e) {
                            // 계산 실패 시 balance 그대로 사용
                            balance = account.balance();
                            log.warn("available 계산 실패, balance 사용: {}", balance);
                        }
                    }
                    break;
                }
            }
            
            // price가 있으면 최대 주문 수량 계산 (balance / price)
            // 소수점 절사하여 정수로 반환 (0.8개 → 0개, 1.2개 → 1개)
            String maxQuantity = null;
            if (price != null && !price.isEmpty()) {
                try {
                    BigDecimal balanceDecimal = new BigDecimal(balance);
                    BigDecimal priceDecimal = new BigDecimal(price);
                    
                    if (priceDecimal.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal quantity = balanceDecimal.divide(priceDecimal, 8, RoundingMode.DOWN);
                        // 소수점 절사하여 정수로 변환
                        maxQuantity = quantity.setScale(0, RoundingMode.DOWN).toPlainString();
                        log.info("업비트 최대 주문 수량 계산 - balance: {}, price: {}, maxQuantity: {} (정수)", balance, price, maxQuantity);
                    } else {
                        log.warn("가격이 0 이하입니다. 최대 주문 수량을 계산할 수 없습니다.");
                    }
                } catch (NumberFormatException e) {
                    log.warn("가격 형식이 올바르지 않습니다. 최대 주문 수량을 계산할 수 없습니다. price: {}", price);
                }
            }
            
            log.info("업비트 최대 주문 정보 조회 완료 - coinType: {}, balance: {}, maxQuantity: {}", coinType, balance, maxQuantity);
            
            return new MaxOrderInfoDTO(balance, maxQuantity);
                    
        } catch (Exception e) {
            log.error("업비트 최대 주문 정보 조회 API 응답 파싱 실패", e);
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
            
            // Feign Client가 DTO로 변환
            return upbitFeignClient.getOrderChance(authorization, market);
            
        } catch (GeneralSecurityException e) {
            log.error("업비트 JWT 생성 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        } catch (feign.FeignException.Unauthorized e) {
            // 401 Unauthorized: API 키 권한 부족
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
            
            // 매도 주문 타입 검증
            if ("limit".equals(orderType)) {
                // 지정가 매도: volume과 price 필요
                if (price == null || price.isEmpty()) {
                    throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
                }
            } else if ("market".equals(orderType)) {
                // 시장가 매도: volume만 필요 (price 불필요)
            } else {
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