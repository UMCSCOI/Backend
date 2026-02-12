package com.example.scoi.domain.invest.client.adapter;

import com.example.scoi.domain.invest.client.ExchangeApiClient;
import com.example.scoi.domain.invest.client.feign.UpbitFeignClient;
import com.example.scoi.domain.invest.dto.InvestResDTO;
import com.example.scoi.domain.invest.dto.MaxOrderInfoDTO;
import com.example.scoi.domain.invest.exception.InvestException;
import com.example.scoi.domain.invest.exception.code.InvestErrorCode;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.member.exception.MemberException;
import com.example.scoi.global.client.dto.ClientErrorDTO;
import com.example.scoi.global.client.dto.UpbitReqDTO;
import com.example.scoi.global.client.dto.UpbitResDTO;
import com.example.scoi.global.util.JwtApiUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpbitApiClient implements ExchangeApiClient {
    
    private final UpbitFeignClient upbitFeignClient;
    private final JwtApiUtil jwtApiUtil; 
    
    @Override
    public MaxOrderInfoDTO getMaxOrderInfo(String phoneNumber, ExchangeType exchangeType, String coinType, String unitPrice, String orderType, String side) {
        try {
            // coinType을 업비트 형식으로 정규화 (KRW-BTC 형식으로 통일)
            String normalizedCoinType = normalizeCoinType(coinType);
            
            String authorization = jwtApiUtil.createUpBitJwt(phoneNumber, null, null);
            log.info("업비트 최대 주문 정보 조회 API 호출 시작 - phoneNumber: {}, coinType: {} (정규화: {}), unitPrice: {}, orderType: {}, side: {}",
                    phoneNumber, coinType, normalizedCoinType, unitPrice, orderType, side);
            
            // Feign Client가 자동으로 List<Account>로 변환해줌 (ObjectMapper 불필요!)
            List<UpbitResDTO.Account> accounts = upbitFeignClient.getAccounts(authorization);
            
            log.info("업비트 최대 주문 정보 조회 API 응답 수신 - 계좌 개수: {}", accounts.size());
            // 디버깅: 실제 응답 값 확인
            for (UpbitResDTO.Account account : accounts) {
                log.info("계좌 정보 - currency: {}, balance: {}, locked: {}, available: {}", 
                        account.currency(), account.balance(), account.locked(), account.available());
            }
            
            return parseMaxOrderInfoResponse(accounts, normalizedCoinType, unitPrice, orderType, side);
            
        } catch (MemberException e) {
            log.error("업비트 API 키를 찾을 수 없습니다 - phoneNumber: {}", phoneNumber, e);
            throw new InvestException(InvestErrorCode.API_KEY_NOT_FOUND);
        } catch (GeneralSecurityException e) {
            log.error("업비트 JWT 생성 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        } catch (FeignException.BadRequest | FeignException.NotFound e) {
            String errorBody = e.contentUTF8();

            // 응답 본문이 있고 JSON 형식인 경우에만 파싱 시도
            if (errorBody != null && !errorBody.isEmpty() && errorBody.trim().startsWith("{")) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ClientErrorDTO.Errors error = objectMapper.readValue(errorBody, ClientErrorDTO.Errors.class);

                    // API 키를 찾을 수 없는 경우
                    if (e instanceof FeignException.NotFound) {
                        throw new InvestException(InvestErrorCode.API_KEY_NOT_FOUND);
                    }

                    // 나머지 400 에러
                    throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
                } catch (Exception parseException) {
                    // JSON 파싱 실패 시 로깅하고 원본 FeignException을 그대로 던짐
                    log.error("UpbitApiClient - 최대 주문 개수 조회 에러 응답 파싱 실패 - status: {}, responseBody: {}, 파싱 에러: {}",
                            e.status(), errorBody, parseException.getMessage());
                    throw e; // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
                }
            } else {
                // 응답 본문이 없거나 JSON이 아닌 경우 - 원본 FeignException을 그대로 던짐
                log.warn("UpbitApiClient - 최대 주문 개수 조회 에러 응답 본문이 비어있거나 JSON 형식이 아님 - status: {}, responseBody: {}",
                        e.status(), errorBody);
                throw e; // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
            }
        } catch (FeignException.Unauthorized e) {
            String errorBody = e.contentUTF8();

            log.error("=== 업비트 API 인증 실패 (401 Unauthorized) ===");
            log.error("phoneNumber: {}, status: {}", phoneNumber, e.status());
            log.error("응답 본문: {}", errorBody);

            // 응답 본문이 있고 JSON 형식인 경우에만 파싱 시도
            if (errorBody != null && !errorBody.isEmpty() && errorBody.trim().startsWith("{")) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ClientErrorDTO.Errors error = objectMapper.readValue(errorBody, ClientErrorDTO.Errors.class);

                    if (error != null && error.error() != null) {
                        String errorName = error.error().name();
                        String errorMessage = error.error().message();

                        log.error("업비트 API 에러 - errorName: {}, errorMessage: {}", errorName, errorMessage);

                        // 권한이 부족한 경우 (API Key 권한 문제)
                        if ("out_of_scope".equals(errorName)) {
                            log.error("⚠️ API Key 권한 부족 - 업비트에서 설정한 API Key 권한이 부족합니다.");
                            throw new InvestException(InvestErrorCode.INSUFFICIENT_API_PERMISSION);
                        }

                        // query_hash 관련 에러
                        if (errorMessage != null && errorMessage.contains("query_hash")) {
                            log.error("⚠️ query_hash mismatch - query_hash 계산이 잘못되었을 수 있습니다.");
                        }
                    }

                    // 나머지 JWT 관련 오류
                    throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
                } catch (InvestException investEx) {
                    throw investEx;
                } catch (Exception parseException) {
                    // JSON 파싱 실패 시 로깅하고 원본 FeignException을 그대로 던짐
                    log.error("UpbitApiClient - 최대 주문 개수 조회 에러 응답 파싱 실패 - status: {}, responseBody: {}, 파싱 에러: {}",
                            e.status(), errorBody, parseException.getMessage());
                    throw e; // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
                }
            } else {
                // 응답 본문이 없거나 JSON이 아닌 경우 - 원본 FeignException을 그대로 던짐
                log.warn("UpbitApiClient - 최대 주문 개수 조회 에러 응답 본문이 비어있거나 JSON 형식이 아님 - status: {}, responseBody: {}",
                        e.status(), errorBody);
                throw e; // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
            }
        } catch (InvestException e) {
            // InvestException은 그대로 전파 (INSUFFICIENT_COIN_AMOUNT, MINIMUM_ORDER_AMOUNT 등)
            throw e;
        } catch (FeignException e) {
            // FeignException은 그대로 전파하여 상위에서 세부적인 분기 가능
            log.error("업비트 최대 주문 정보 조회 API 호출 실패 - FeignException: status: {}", e.status(), e);
            throw e;
        } catch (Exception e) {
            // FeignException이 아닌 경우에만 InvestException으로 변환
            log.error("업비트 최대 주문 정보 조회 API 호출 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }
    
    private MaxOrderInfoDTO parseMaxOrderInfoResponse(List<UpbitResDTO.Account> accounts, String coinType, String unitPrice, String orderType, String side) {
        try {
            String currency;
            String targetCurrency; // 조회할 화폐 (KRW 또는 코인)
            
            // 시장가 주문인 경우
            boolean isMarketOrder = "price".equals(orderType) || "market".equals(orderType);
            
            if (isMarketOrder) {
                // side 파라미터를 기준으로 매수/매도 판단
                if ("bid".equals(side)) {
                    // 매수: KRW 잔액 조회
                    targetCurrency = "KRW";
                    currency = "KRW";
                    log.info("업비트 시장가 매수 - {}를 매수하기 위해 KRW 잔액 조회", coinType);
                } else if ("ask".equals(side)) {
                    // 매도: 코인 잔액 조회
                    if (coinType.contains("-")) {
                        String[] parts = coinType.split("-");
                        targetCurrency = parts[1]; // KRW-BTC -> BTC
                        currency = parts[1];
                    } else {
                        targetCurrency = coinType;
                        currency = coinType;
                    }
                    log.info("업비트 시장가 매도 - {} 잔액 조회", targetCurrency);
                } else {
                    // side가 없거나 잘못된 경우 기본값 (매수)
                    targetCurrency = "KRW";
                    currency = "KRW";
                    log.warn("업비트 시장가 주문 - side 파라미터가 없거나 잘못됨 ({}), 기본값으로 매수 처리", side);
                }
            } else {
                // 지정가 주문: side 파라미터를 기준으로 매수/매도 판단
                if ("bid".equals(side)) {
                    // 지정가 매수: KRW 잔액 조회
                    targetCurrency = "KRW";
                    currency = "KRW";
                    log.info("업비트 지정가 매수 - {}를 매수하기 위해 KRW 잔액 조회", coinType);
                } else if ("ask".equals(side)) {
                    // 지정가 매도: 코인 잔액 조회
                    if (coinType.contains("-")) {
                        String[] parts = coinType.split("-");
                        targetCurrency = parts[1]; // KRW-USDT -> USDT
                        currency = parts[1];
                    } else {
                        targetCurrency = coinType;
                        currency = coinType;
                    }
                    log.info("업비트 지정가 매도 - {} 잔액 조회", targetCurrency);
                } else {
                    // side가 없거나 잘못된 경우 기본값 (매수)
                    targetCurrency = "KRW";
                    currency = "KRW";
                    log.warn("업비트 지정가 주문 - side 파라미터가 없거나 잘못됨 ({}), 기본값으로 매수 처리", side);
                }
            }
            
            String balance = "0";
            
            // 해당 currency로 계좌 찾기
            for (UpbitResDTO.Account account : accounts) {
                if (currency.equals(account.currency())) {
                    // available이 있으면 available 사용 (매수 가능 금액)
                    // 주의: 업비트 API /v1/accounts는 available 필드를 제공하지 않음 (공식 문서 확인)
                    // 따라서 available이 null이면 balance - locked로 계산해야 함
                    if (account.available() != null && !account.available().isEmpty()) {
                        balance = account.available();
                    } else if (account.balance() != null && !account.balance().isEmpty()) {
                        // available이 null이면 balance - locked로 계산
                        try {
                            BigDecimal balanceDecimal = new BigDecimal(account.balance());
                            BigDecimal lockedDecimal = account.locked() != null && !account.locked().isEmpty() 
                                    ? new BigDecimal(account.locked()) 
                                    : BigDecimal.ZERO;
                            BigDecimal availableDecimal = balanceDecimal.subtract(lockedDecimal);
                            balance = availableDecimal.toPlainString();
                        } catch (NumberFormatException e) {
                            // 계산 실패 시 balance 그대로 사용
                            balance = account.balance();
                            log.warn("available 계산 실패, balance 사용: {}", balance);
                        }
                    }
                    break;
                }
            }
            
            String maxQuantity = null;
            
            // 시장가 주문 처리
            if (isMarketOrder) {
                if ("bid".equals(side)) {
                    // 시장가 매수: 현재가 조회하여 대략적인 수량 계산
                    try {
                        List<UpbitResDTO.Ticker> tickers = upbitFeignClient.getTicker(coinType);
                        
                        if (tickers != null && !tickers.isEmpty()) {
                            UpbitResDTO.Ticker ticker = tickers.get(0);
                            if (ticker.trade_price() != null && ticker.trade_price() > 0) {
                                BigDecimal balanceDecimal = new BigDecimal(balance);
                                BigDecimal currentPrice = BigDecimal.valueOf(ticker.trade_price());
                                BigDecimal quantity = balanceDecimal.divide(currentPrice, 8, RoundingMode.DOWN);
                                // 소수점 절사하여 정수로 변환
                                maxQuantity = quantity.setScale(0, RoundingMode.DOWN).toPlainString();
                                log.info("업비트 시장가 매수 - KRW 잔액: {}, 현재가: {}, 최대 매수 가능 수량: {} (정수)", 
                                        balance, currentPrice, maxQuantity);
                            } else {
                                log.warn("업비트 현재가 조회 실패 또는 가격이 0 이하 - market: {}", coinType);
                                maxQuantity = null;
                            }
                        } else {
                            log.warn("업비트 현재가 조회 실패 - 응답이 비어있음: {}", coinType);
                            maxQuantity = null;
                        }
                    } catch (Exception e) {
                        log.warn("업비트 현재가 조회 실패 - 시장가 매수 수량 계산 불가: {}", e.getMessage());
                        maxQuantity = null;
                    }
                } else if ("ask".equals(side)) {
                    // 시장가 매도: 코인 잔액을 maxQuantity로 반환 (최대 매도 가능 수량)
                    // 현재가 조회하여 최소 주문 금액 검증
                    try {
                        BigDecimal balanceDecimal = new BigDecimal(balance);
                        
                        // 현재가 조회하여 최소 주문 금액 검증
                        try {
                            List<UpbitResDTO.Ticker> tickers = upbitFeignClient.getTicker(coinType);
                            if (tickers != null && !tickers.isEmpty()) {
                                UpbitResDTO.Ticker ticker = tickers.get(0);
                                if (ticker.trade_price() != null && ticker.trade_price() > 0) {
                                    BigDecimal currentPrice = BigDecimal.valueOf(ticker.trade_price());
                                    BigDecimal orderAmount = currentPrice.multiply(balanceDecimal);
                                    BigDecimal minOrderAmount = getUpbitMinimumOrderAmount(normalizeCoinType(coinType));
                                    
                                    if (orderAmount.compareTo(minOrderAmount) < 0) {
                                        log.warn("업비트 시장가 매도 - 최소 주문 금액 미만 - 주문 금액: {}, 최소 주문 금액: {}", 
                                                orderAmount, minOrderAmount);
                                        Map<String, String> errorDetails = Map.of(
                                            "orderAmount", orderAmount.toPlainString(),
                                            "minTotal", minOrderAmount.toPlainString()
                                        );
                                        throw new InvestException(InvestErrorCode.MINIMUM_ORDER_AMOUNT, errorDetails);
                                    }
                                    
                                    log.info("업비트 시장가 매도 - 최소 주문 금액 검증 통과 - balance: {}, 현재가: {}, 주문 금액: {}, 최소 주문 금액: {}", 
                                            balance, currentPrice, orderAmount, minOrderAmount);
                                }
                            }
                        } catch (InvestException e) {
                            // MINIMUM_ORDER_AMOUNT 예외는 그대로 전파
                            throw e;
                        } catch (Exception e) {
                            log.warn("업비트 시장가 매도 현재가 조회 실패 - 최소 주문 금액 검증을 생략합니다: {}", e.getMessage());
                        }
                        
                        // 소수점 절사하여 정수로 변환
                        maxQuantity = balanceDecimal.setScale(0, RoundingMode.DOWN).toPlainString();
                        log.info("업비트 시장가 매도 - 코인 잔액: {}, 최대 매도 가능 수량: {} (정수)", balance, maxQuantity);
                    } catch (NumberFormatException e) {
                        log.warn("업비트 시장가 매도 - 잔액 파싱 실패, 원본 값 사용: {}", balance);
                        maxQuantity = balance;
                    }
                } else {
                    log.warn("업비트 시장가 주문 - side 파라미터가 없거나 잘못됨 ({}), maxQuantity: null", side);
                    maxQuantity = null;
                }
            } else {
                // 지정가 주문
                if ("ask".equals(side)) {
                    // 지정가 매도: 코인 보유 여부 확인
                    BigDecimal balanceDecimal;
                    try {
                        balanceDecimal = new BigDecimal(balance);
                    } catch (NumberFormatException e) {
                        balanceDecimal = BigDecimal.ZERO;
                    }
                    
                    // 코인이 없으면 maxQuantity를 0으로 설정 (시장가 매도와 동일하게 처리)
                    if (balanceDecimal.compareTo(BigDecimal.ZERO) <= 0) {
                        log.warn("업비트 지정가 매도 - 보유 수량 없음 - coinType: {}, balance: {}", coinType, balance);
                        maxQuantity = "0";
                    } else {
                        // 매도는 보유 수량이 maxQuantity
                        maxQuantity = balanceDecimal.setScale(0, RoundingMode.DOWN).toPlainString();
                        
                        // unitPrice가 있으면 최소 주문 금액 검증
                        if (unitPrice != null && !unitPrice.isEmpty()) {
                            try {
                                BigDecimal unitPriceDecimal = new BigDecimal(unitPrice);
                                
                                if (unitPriceDecimal.compareTo(BigDecimal.ZERO) > 0) {
                                    // 최소 주문 금액 검증: unitPrice * balance >= 5000원
                                    BigDecimal maxOrderAmount = unitPriceDecimal.multiply(balanceDecimal);
                                    BigDecimal minOrderAmount = getUpbitMinimumOrderAmount(normalizeCoinType(coinType));
                                    
                                    if (maxOrderAmount.compareTo(minOrderAmount) < 0) {
                                        log.warn("업비트 지정가 매도 - 최소 주문 금액 미만 - 주문 금액: {}, 최소 주문 금액: {}", 
                                                maxOrderAmount, minOrderAmount);
                                        Map<String, String> errorDetails = Map.of(
                                            "orderAmount", maxOrderAmount.toPlainString(),
                                            "minTotal", minOrderAmount.toPlainString()
                                        );
                                        throw new InvestException(InvestErrorCode.MINIMUM_ORDER_AMOUNT, errorDetails);
                                    }
                                    
                                    log.info("업비트 지정가 매도 - 최소 주문 금액 검증 통과 - balance: {}, unitPrice: {}, maxQuantity: {}, 주문 금액: {}, 최소 주문 금액: {}", 
                                            balance, unitPrice, maxQuantity, maxOrderAmount, minOrderAmount);
                                } else {
                                    log.warn("단위 가격이 0 이하입니다. 최소 주문 금액 검증을 할 수 없습니다.");
                                }
                            } catch (InvestException e) {
                                // MINIMUM_ORDER_AMOUNT, INSUFFICIENT_COIN_AMOUNT 예외는 그대로 전파
                                throw e;
                            } catch (NumberFormatException e) {
                                log.warn("단위 가격 형식이 올바르지 않습니다. 최소 주문 금액 검증을 할 수 없습니다. unitPrice: {}", unitPrice);
                            }
                        }
                    }
                } else {
                    // 지정가 매수
                    if (unitPrice != null && !unitPrice.isEmpty()) {
                        try {
                            BigDecimal balanceDecimal = new BigDecimal(balance);
                            BigDecimal unitPriceDecimal = new BigDecimal(unitPrice);
                            
                            if (unitPriceDecimal.compareTo(BigDecimal.ZERO) > 0) {
                                // 단가가 잔고보다 크면 잔고 부족 에러
                                if (unitPriceDecimal.compareTo(balanceDecimal) > 0) {
                                    log.warn("업비트 지정가 매수 - 잔고 부족 - 잔고: {}, 단가: {}", balance, unitPrice);
                                    Map<String, String> errorDetails = Map.of(
                                        "balance", balance,
                                        "requiredAmount", unitPrice,
                                        "shortage", unitPriceDecimal.subtract(balanceDecimal).toPlainString()
                                    );
                                    throw new InvestException(InvestErrorCode.INSUFFICIENT_BALANCE, errorDetails);
                                }
                                
                                BigDecimal quantity = balanceDecimal.divide(unitPriceDecimal, 8, RoundingMode.DOWN);
                                // 소수점 절사하여 정수로 변환
                                maxQuantity = quantity.setScale(0, RoundingMode.DOWN).toPlainString();
                                log.info("업비트 최대 주문 수량 계산 - balance: {}, unitPrice: {}, maxQuantity: {} (정수)", 
                                        balance, unitPrice, maxQuantity);
                            } else {
                                log.warn("단위 가격이 0 이하입니다. 최대 주문 수량을 계산할 수 없습니다.");
                            }
                        } catch (InvestException e) {
                            // INSUFFICIENT_BALANCE 예외는 그대로 전파
                            throw e;
                        } catch (NumberFormatException e) {
                            log.warn("단위 가격 형식이 올바르지 않습니다. 최대 주문 수량을 계산할 수 없습니다. unitPrice: {}", unitPrice);
                        }
                    }
                }
            }
            
            log.info("업비트 최대 주문 정보 조회 완료 - coinType: {}, balance: {}, maxQuantity: {}, orderType: {}", 
                    coinType, balance, maxQuantity, orderType);
            
            return new MaxOrderInfoDTO(balance, maxQuantity);
                    
        } catch (InvestException e) {
            // InvestException은 그대로 전파 (INSUFFICIENT_COIN_AMOUNT, MINIMUM_ORDER_AMOUNT 등)
            throw e;
        } catch (Exception e) {
            log.error("업비트 최대 주문 정보 조회 API 응답 파싱 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
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
        
        // 단순 코인 심볼만 오는 경우 (예: "USDC", "BTC") -> "KRW-USDC", "KRW-BTC"로 변환
        if (!coinType.contains("-") && !coinType.contains("_")) {
            return "KRW-" + coinType;
        }

        // KRW만 있는 경우
        if ("KRW".equals(coinType)) {
            return "KRW-BTC";
        }
        
        // 코인 타입만 있는 경우 (예: USDC, BTC) -> KRW-XXX 형식으로 변환
        // 업비트 API는 KRW-USDC 형식을 사용
        return "KRW-" + coinType;
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
            // market을 업비트 형식으로 정규화 (KRW-BTC 형식으로 통일)
            String normalizedMarket = normalizeCoinType(market);
            
            log.info("업비트 주문 가능 여부 확인 API 호출 시작 - phoneNumber: {}, market: {} (정규화: {}), side: {}",
                    phoneNumber, market, normalizedMarket, side);

            UpbitResDTO.OrderChance orderChance = getOrderChance(phoneNumber, normalizedMarket);
            validateOrderAvailability(normalizedMarket, side, orderType, price, volume, orderChance);
            
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
            
            log.info("업비트 API 호출 시작 - market: {}", market);
            // Feign Client가 DTO로 변환
            UpbitResDTO.OrderChance orderChance = upbitFeignClient.getOrderChance(authorization, market);
            
            log.info("업비트 API 응답 수신 완료");
            
            // 전체 응답 구조 확인 (디버깅용)
            log.info("업비트 API 응답 전체 구조 - orderChance: {}", orderChance);
            log.info("업비트 API 응답 - bid_fee: {}, ask_fee: {}, market: {}, bid: {}, ask: {}, bid_account: {}, ask_account: {}", 
                    orderChance.bid_fee(), orderChance.ask_fee(), orderChance.market(), 
                    orderChance.bid(), orderChance.ask(), orderChance.bid_account(), orderChance.ask_account());
            
            // API 응답에서 실제 마켓 형식 확인
            if (orderChance.market() != null && orderChance.market().id() != null) {
                log.info("업비트 API 응답 market.id: {} (요청한 market: {})", orderChance.market().id(), market);
            }
            
            // bid 객체 확인 (최소 주문 금액 검증용)
            if (orderChance.bid() != null) {
                log.info("업비트 API 응답 bid 객체 존재 - currency: {}, min_total: {}", 
                        orderChance.bid().currency(), orderChance.bid().min_total());
            } else {
                log.warn("업비트 API 응답 bid 객체가 null입니다! - 최소 주문 금액 검증 불가");
                // ask 객체도 확인
                if (orderChance.ask() != null) {
                    log.info("업비트 API 응답 ask 객체 존재 - currency: {}, min_total: {}", 
                            orderChance.ask().currency(), orderChance.ask().min_total());
                } else {
                    log.warn("업비트 API 응답 ask 객체도 null입니다!");
                }
            }
            
            return orderChance;
            
        } catch (MemberException e) {
            log.error("업비트 API 키를 찾을 수 없습니다 - phoneNumber: {}", phoneNumber, e);
            throw new InvestException(InvestErrorCode.API_KEY_NOT_FOUND);
        } catch (GeneralSecurityException e) {
            log.error("업비트 JWT 생성 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        } catch (FeignException.BadRequest | FeignException.NotFound e) {
            String errorBody = e.contentUTF8();

            // 응답 본문이 있고 JSON 형식인 경우에만 파싱 시도
            if (errorBody != null && !errorBody.isEmpty() && errorBody.trim().startsWith("{")) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ClientErrorDTO.Errors error = objectMapper.readValue(errorBody, ClientErrorDTO.Errors.class);

                    // API 키를 찾을 수 없는 경우
                    if (e instanceof FeignException.NotFound) {
                        throw new InvestException(InvestErrorCode.API_KEY_NOT_FOUND);
                    }

                    // 나머지 400 에러
                    throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
                } catch (Exception parseException) {
                    // JSON 파싱 실패 시 로깅하고 원본 FeignException을 그대로 던짐
                    log.error("UpbitApiClient - 주문 가능여부 조회 에러 응답 파싱 실패 - status: {}, responseBody: {}, 파싱 에러: {}",
                            e.status(), errorBody, parseException.getMessage());
                    throw e; // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
                }
            } else {
                // 응답 본문이 없거나 JSON이 아닌 경우 - 원본 FeignException을 그대로 던짐
                log.warn("UpbitApiClient - 주문 가능여부 조회 에러 응답 본문이 비어있거나 JSON 형식이 아님 - status: {}, responseBody: {}",
                        e.status(), errorBody);
                throw e; // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
            }
        } catch (FeignException.Unauthorized e) {
            String errorBody = e.contentUTF8();

            // 응답 본문이 있고 JSON 형식인 경우에만 파싱 시도
            if (errorBody != null && !errorBody.isEmpty() && errorBody.trim().startsWith("{")) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ClientErrorDTO.Errors error = objectMapper.readValue(errorBody, ClientErrorDTO.Errors.class);

                    if (error != null && error.error() != null) {
                        String errorName = error.error().name();

                        // 권한이 부족한 경우
                        if ("out_of_scope".equals(errorName)) {
                            throw new InvestException(InvestErrorCode.INSUFFICIENT_API_PERMISSION);
                        }
                    }

                    // 나머지 JWT 관련 오류
                    throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
                } catch (Exception parseException) {
                    // JSON 파싱 실패 시 로깅하고 원본 FeignException을 그대로 던짐
                    log.error("UpbitApiClient - 주문 가능여부 조회 에러 응답 파싱 실패 - status: {}, responseBody: {}, 파싱 에러: {}",
                            e.status(), errorBody, parseException.getMessage());
                    throw e; // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
                }
            } else {
                // 응답 본문이 없거나 JSON이 아닌 경우 - 원본 FeignException을 그대로 던짐
                log.warn("UpbitApiClient - 주문 가능여부 조회 에러 응답 본문이 비어있거나 JSON 형식이 아님 - status: {}, responseBody: {}",
                        e.status(), errorBody);
                throw e; // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
            }
        } catch (FeignException e) {
            // FeignException은 그대로 전파하여 상위에서 세부적인 분기 가능
            log.error("업비트 주문 가능 정보 조회 실패 - FeignException: status: {}", e.status(), e);
            throw e;
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
            
            // 1단계: 최소 주문 금액 검증 (주문 금액이 최소 주문 금액을 넘는지 확인)
            log.info("업비트 최소 주문 금액 검증 시작 - 주문 금액: {}", requiredAmount);
            
            // 업비트 API 문서에 따르면 market.bid.min_total 또는 market.ask.min_total에 최소 주문 금액이 있음
            // 참고: https://docs.upbit.com/kr/reference/available-order-information
            UpbitResDTO.Bid bid = null;
            String minTotalStr = null;
            
            // 1순위: orderChance.bid() 확인
            if (orderChance.bid() != null && orderChance.bid().min_total() != null && !orderChance.bid().min_total().isEmpty()) {
                bid = orderChance.bid();
                minTotalStr = bid.min_total();
                log.info("업비트 최소 주문 금액 검증 - orderChance.bid()에서 조회: {}", minTotalStr);
            }
            // 2순위: orderChance.market().bid() 확인
            else if (orderChance.market() != null && orderChance.market().bid() != null 
                    && orderChance.market().bid().min_total() != null 
                    && !orderChance.market().bid().min_total().isEmpty()) {
                bid = orderChance.market().bid();
                minTotalStr = bid.min_total();
                log.info("업비트 최소 주문 금액 검증 - orderChance.market().bid()에서 조회: {}", minTotalStr);
            }
            
            log.info("업비트 최소 주문 금액 검증 - bid 객체: {}, min_total: {}", 
                    bid != null ? "존재" : "null", 
                    minTotalStr != null ? minTotalStr : "null");
            
            BigDecimal minTotal;
            String minTotalSource;
            
            if (minTotalStr != null && !minTotalStr.isEmpty()) {
                // API에서 제공하는 최소 주문 금액 사용
                minTotal = new BigDecimal(minTotalStr);
                minTotalSource = "API 응답";
                log.info("업비트 최소 주문 금액 검증 - API 응답에서 최소 주문 금액 조회: {}", minTotal);
            } else {
                // 업비트 API가 최소 주문 금액을 제공하지 않으므로 마켓 타입에 따라 기본값 사용
                minTotal = getUpbitMinimumOrderAmount(market);
                minTotalSource = "마켓별 기본값";
                log.warn("업비트 API 응답에 최소 주문 금액 정보가 없어 마켓별 기본값 사용 - market: {}, minTotal: {}, bid: {}, min_total: {}", 
                        market, minTotal, 
                        bid != null ? "존재" : "null", 
                        minTotalStr != null ? minTotalStr : "null 또는 빈 문자열");
            }
            
            log.info("업비트 최소 주문 금액 검증 - 주문 금액: {}, 최소 주문 금액: {} ({})", requiredAmount, minTotal, minTotalSource);
            if (requiredAmount.compareTo(minTotal) < 0) {
                // 주문 금액이 최소 주문 금액보다 낮으면 에러 발생
                log.warn("주문 금액이 최소 주문 금액보다 낮음 - 주문 금액: {}, 최소 주문 금액: {}", requiredAmount, minTotal);
                Map<String, String> errorDetails = Map.of(
                    "requiredAmount", requiredAmountStr,
                    "minTotal", minTotal.toPlainString()
                );
                throw new InvestException(InvestErrorCode.MINIMUM_ORDER_AMOUNT, errorDetails);
            }
            log.info("업비트 최소 주문 금액 검증 통과 - 주문 금액: {} >= 최소 주문 금액: {}", requiredAmount, minTotal);
            
            // 2단계: 잔고 검증 (최소 주문 금액을 넘는다면, 잔고로 살 수 있는지 확인)
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
            BigDecimal volumeDecimal = new BigDecimal(volume);
            BigDecimal orderAmount = null; // 주문 금액 (최소 주문 금액 검증용)
            
            if ("limit".equals(orderType)) {
                // 지정가 매도: volume과 price 필요
                if (price == null || price.isEmpty()) {
                    throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
                }
                BigDecimal priceDecimal = new BigDecimal(price);
                orderAmount = priceDecimal.multiply(volumeDecimal);
            } else if ("market".equals(orderType)) {
                // 시장가 매도: volume만 필요 (price 불필요)
                // 현재가 조회 후 (현재가 × 수량)으로 주문 금액 계산하여 최소 주문 금액과 비교
                try {
                    log.info("업비트 시장가 매도 주문 금액 계산을 위한 현재가 조회 시작 - market: {}", market);
                    List<UpbitResDTO.Ticker> tickers = upbitFeignClient.getTicker(market);
                    if (tickers != null && !tickers.isEmpty()) {
                        UpbitResDTO.Ticker ticker = tickers.get(0);
                        if (ticker.trade_price() != null && ticker.trade_price() > 0) {
                            BigDecimal currentPrice = BigDecimal.valueOf(ticker.trade_price());
                            orderAmount = currentPrice.multiply(volumeDecimal);
                            log.info("업비트 시장가 매도 주문 금액 계산 - 현재가: {}, 수량: {}, 주문 금액: {}", currentPrice, volumeDecimal, orderAmount);
                        } else {
                            log.warn("업비트 시장가 매도 현재가 조회 실패 또는 가격이 0 이하 - 최소 주문 금액 검증을 생략합니다. market: {}", market);
                        }
                    } else {
                        log.warn("업비트 시장가 매도 현재가 조회 실패 - 응답이 비어있음, 최소 주문 금액 검증을 생략합니다. market: {}", market);
                    }
                } catch (Exception e) {
                    log.warn("업비트 시장가 매도 주문 금액 계산 실패 - 최소 주문 금액 검증을 생략합니다: {}", e.getMessage());
                }
            } else {
                throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
            }

            requiredAmountStr = volume; // 매도 시 필요한 수량
            
            // 지정가/시장가 매도: 최소 주문 금액 검증 (orderAmount가 계산된 경우에만)
            if (orderAmount != null) {
                // 업비트 API 문서에 따르면 market.ask.min_total에 최소 주문 금액이 있음
                // 참고: https://docs.upbit.com/kr/reference/available-order-information
                UpbitResDTO.Ask ask = null;
                String minTotalStr = null;
                
                // 1순위: orderChance.ask() 확인
                if (orderChance.ask() != null && orderChance.ask().min_total() != null && !orderChance.ask().min_total().isEmpty()) {
                    ask = orderChance.ask();
                    minTotalStr = ask.min_total();
                    log.info("업비트 매도 최소 주문 금액 검증 - orderChance.ask()에서 조회: {}", minTotalStr);
                }
                // 2순위: orderChance.market().ask() 확인
                else if (orderChance.market() != null && orderChance.market().ask() != null 
                        && orderChance.market().ask().min_total() != null 
                        && !orderChance.market().ask().min_total().isEmpty()) {
                    ask = orderChance.market().ask();
                    minTotalStr = ask.min_total();
                    log.info("업비트 매도 최소 주문 금액 검증 - orderChance.market().ask()에서 조회: {}", minTotalStr);
                }
                
                BigDecimal minTotal;
                String minTotalSource;
                
                if (minTotalStr != null && !minTotalStr.isEmpty()) {
                    // API에서 제공하는 최소 주문 금액 사용
                    minTotal = new BigDecimal(minTotalStr);
                    minTotalSource = "API 응답";
                    log.info("업비트 매도 최소 주문 금액 검증 - API 응답에서 최소 주문 금액 조회: {}", minTotal);
                } else {
                    // 업비트 API가 최소 주문 금액을 제공하지 않으므로 마켓 타입에 따라 기본값 사용
                    minTotal = getUpbitMinimumOrderAmount(market);
                    minTotalSource = "마켓별 기본값";
                    log.warn("업비트 API 응답에 최소 주문 금액 정보가 없어 마켓별 기본값 사용 - market: {}, minTotal: {}, ask: {}, min_total: {}", 
                            market, minTotal,
                            ask != null ? "존재" : "null", 
                            minTotalStr != null ? minTotalStr : "null 또는 빈 문자열");
                }
                
                log.info("업비트 매도 최소 주문 금액 검증 - 주문 금액: {}, 최소 주문 금액: {} ({})", orderAmount, minTotal, minTotalSource);
                if (orderAmount.compareTo(minTotal) < 0) {
                    log.warn("주문 금액이 최소 주문 금액보다 낮음 - 주문 금액: {}, 최소 주문 금액: {}", orderAmount, minTotal);
                    Map<String, String> errorDetails = Map.of(
                        "orderAmount", orderAmount.toPlainString(),
                        "minTotal", minTotal.toPlainString()
                    );
                    throw new InvestException(InvestErrorCode.MINIMUM_ORDER_AMOUNT, errorDetails);
                }
                log.info("업비트 매도 최소 주문 금액 검증 통과 - 주문 금액: {} >= 최소 주문 금액: {}", orderAmount, minTotal);
            }
            
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

    @Override
    public InvestResDTO.OrderDTO testCreateOrder(
            String phoneNumber,
            ExchangeType exchangeType,
            String market,
            String side,
            String orderType,
            String price,
            String volume
    ) {
        try {
            // market을 업비트 형식으로 정규화 (KRW-USDC 형식으로 통일)
            String normalizedMarket = normalizeCoinType(market);

            log.info("업비트 주문 생성 테스트 API 호출 시작 - phoneNumber: {}, market: {} (정규화: {}), side: {}, orderType: {}",
                    phoneNumber, market, normalizedMarket, side, orderType);

            // 주문 생성 요청 DTO 생성 (빈 문자열은 null로 변환하여 JSON에 포함되지 않도록 함)
            UpbitReqDTO.CreateOrder request =
                    UpbitReqDTO.CreateOrder.builder()
                            .market(normalizedMarket)
                            .side(side)
                            .ord_type(orderType)
                            .price((price != null && !price.isEmpty()) ? price : null)
                            .volume((volume != null && !volume.isEmpty()) ? volume : null)
                            .build();

            log.info("업비트 주문 생성 테스트 요청 정보 - market: {}, side: {}, ord_type: {}, price: {}, volume: {}",
                    request.market(), request.side(), request.ord_type(), request.price(), request.volume());

            // 요청 DTO 검증
            if (request.market() == null || request.market().isEmpty()) {
                log.error("업비트 주문 생성 테스트 실패 - market이 null이거나 비어있음");
                throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
            }
            if (request.side() == null || request.side().isEmpty()) {
                log.error("업비트 주문 생성 테스트 실패 - side가 null이거나 비어있음");
                throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
            }
            if (request.ord_type() == null || request.ord_type().isEmpty()) {
                log.error("업비트 주문 생성 테스트 실패 - ord_type이 null이거나 비어있음");
                throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
            }

            // JWT 생성 (POST 요청이므로 body 사용)
            log.info("업비트 JWT 생성 시작 - body를 query string으로 변환하여 query_hash 계산");
            String authorization = jwtApiUtil.createUpBitJwt(phoneNumber, null, request);
            log.info("업비트 JWT 생성 완료 - Authorization 헤더 길이: {}", authorization.length());

            // 업비트 주문 생성 테스트 API 호출 (실제 주문 생성 없이 검증)
            UpbitResDTO.CreateOrder testResponse = upbitFeignClient.testCreateOrder(authorization, request);

            // 응답을 InvestResDTO.OrderDTO로 변환
            return new InvestResDTO.OrderDTO(
                    testResponse.uuid(),
                    testResponse.uuid(), // 업비트는 txid가 없으므로 uuid 사용
                    testResponse.market(),
                    testResponse.side(),
                    testResponse.ord_type(),
                    parseCreatedAt(testResponse.created_at())
            );

        } catch (MemberException e) {
            log.error("업비트 API 키를 찾을 수 없습니다 - phoneNumber: {}", phoneNumber, e);
            throw new InvestException(InvestErrorCode.API_KEY_NOT_FOUND);
        } catch (GeneralSecurityException e) {
            log.error("업비트 JWT 생성 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        } catch (FeignException.Unauthorized e) {
            String errorBody = e.contentUTF8();
            if (errorBody != null && !errorBody.isEmpty() && errorBody.trim().startsWith("{")) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ClientErrorDTO.Errors error = objectMapper.readValue(errorBody, ClientErrorDTO.Errors.class);
                    if (error != null && error.error() != null) {
                        log.error("=== 업비트 주문 생성 테스트 실패 (401) ===");
                        log.error("에러 이름: {}", error.error().name());
                        log.error("에러 메시지: {}", error.error().message());
                        log.error("전체 응답: {}", errorBody);
                    }
                } catch (Exception parseException) {
                    log.error("업비트 주문 생성 테스트 실패 (401) - JSON 파싱 실패: {}, responseBody: {}",
                            parseException.getMessage(), errorBody);
                }
            }
            throw new InvestException(InvestErrorCode.INSUFFICIENT_API_PERMISSION);
        } catch (FeignException.BadRequest e) {
            String errorBody = e.contentUTF8();
            if (errorBody != null && !errorBody.isEmpty() && errorBody.trim().startsWith("{")) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ClientErrorDTO.Errors error = objectMapper.readValue(errorBody, ClientErrorDTO.Errors.class);
                    if (error != null && error.error() != null) {
                        log.error("=== 업비트 주문 생성 테스트 실패 (400) ===");
                        log.error("에러 이름: {}", error.error().name());
                        log.error("에러 메시지: {}", error.error().message());
                        log.error("전체 응답: {}", errorBody);
                    } else {
                        log.error("업비트 주문 생성 테스트 실패 (400) - responseBody: {}", errorBody);
                    }
                } catch (Exception parseException) {
                    log.error("업비트 주문 생성 테스트 실패 (400) - JSON 파싱 실패: {}, responseBody: {}",
                            parseException.getMessage(), errorBody);
                }
            } else {
                log.error("업비트 주문 생성 테스트 실패 (400) - HTML 응답 또는 비JSON 응답: {}", errorBody);
            }
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        } catch (InvestException e) {
            throw e;
        } catch (Exception e) {
            log.error("업비트 주문 생성 테스트 API 호출 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }

    @Override
    public InvestResDTO.OrderDTO createOrder(
            String phoneNumber,
            ExchangeType exchangeType,
            String market,
            String side,
            String orderType,
            String price,
            String volume,
            String password
    ) {
        try {
            // market을 업비트 형식으로 정규화 (KRW-USDC 형식으로 통일)
            String normalizedMarket = normalizeCoinType(market);

            log.info("업비트 주문 생성 API 호출 시작 - phoneNumber: {}, market: {} (정규화: {}), side: {}, orderType: {}",
                    phoneNumber, market, normalizedMarket, side, orderType);

            // 주문 생성 요청 DTO 생성 (빈 문자열은 null로 변환하여 JSON에 포함되지 않도록 함)
            UpbitReqDTO.CreateOrder request =
                    UpbitReqDTO.CreateOrder.builder()
                            .market(normalizedMarket)
                            .side(side)
                            .ord_type(orderType)
                            .price((price != null && !price.isEmpty()) ? price : null)
                            .volume((volume != null && !volume.isEmpty()) ? volume : null)
                            .build();

            // JWT 생성 (POST 요청이므로 body 사용)
            log.info("업비트 주문 생성 JWT 생성 시작 - body를 query string으로 변환하여 query_hash 계산");
            log.info("업비트 주문 생성 요청 DTO 상세 - market: {}, side: {}, ord_type: {}, price: {}, volume: {}",
                    request.market(), request.side(), request.ord_type(), request.price(), request.volume());
            String authorization = jwtApiUtil.createUpBitJwt(phoneNumber, null, request);

            // 주문 생성 테스트 API 호출 (실제 주문 생성 없이 검증)
            try {
                UpbitResDTO.CreateOrder testResponse = upbitFeignClient.testCreateOrder(authorization, request);
            } catch (FeignException.BadRequest | FeignException.NotFound e) {
                String errorBody = e.contentUTF8();

                // 응답 본문이 있고 JSON 형식인 경우에만 파싱 시도
                if (errorBody != null && !errorBody.isEmpty() && errorBody.trim().startsWith("{")) {
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        ClientErrorDTO.Errors error = objectMapper.readValue(errorBody, ClientErrorDTO.Errors.class);
                        if (error != null && error.error() != null) {
                            log.error("UpbitApiClient - 주문 생성 테스트 에러 응답 - status: {}, errorName: {}, errorMessage: {}",
                                    e.status(), error.error().name(), error.error().message());
                        } else {
                            log.error("UpbitApiClient - 주문 생성 테스트 에러 응답 - status: {}, responseBody: {}",
                                    e.status(), errorBody);
                        }
                    } catch (Exception parseException) {
                        // JSON 파싱 실패 시 로깅
                        log.error("UpbitApiClient - 주문 생성 테스트 에러 응답 파싱 실패 - status: {}, responseBody: {}, 파싱 에러: {}",
                                e.status(), errorBody, parseException.getMessage());
                    }
                } else {
                    // 응답 본문이 없거나 JSON이 아닌 경우
                    log.warn("UpbitApiClient - 주문 생성 테스트 에러 응답 본문이 비어있거나 JSON 형식이 아님 - status: {}, responseBody: {}",
                            e.status(), errorBody);
                }
                // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
                throw e;
            } catch (FeignException.Unauthorized e) {
                String errorBody = e.contentUTF8();

                // 응답 본문이 있고 JSON 형식인 경우에만 파싱 시도
                if (errorBody != null && !errorBody.isEmpty() && errorBody.trim().startsWith("{")) {
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        ClientErrorDTO.Errors error = objectMapper.readValue(errorBody, ClientErrorDTO.Errors.class);
                        if (error != null && error.error() != null) {
                            log.error("UpbitApiClient - 주문 생성 테스트 에러 응답 - status: {}, errorName: {}, errorMessage: {}",
                                    e.status(), error.error().name(), error.error().message());
                        } else {
                            log.error("UpbitApiClient - 주문 생성 테스트 에러 응답 - status: {}, responseBody: {}",
                                    e.status(), errorBody);
                        }
                    } catch (Exception parseException) {
                        // JSON 파싱 실패 시 로깅
                        log.error("UpbitApiClient - 주문 생성 테스트 에러 응답 파싱 실패 - status: {}, responseBody: {}, 파싱 에러: {}",
                                e.status(), errorBody, parseException.getMessage());
                    }
                } else {
                    // 응답 본문이 없거나 JSON이 아닌 경우
                    log.warn("UpbitApiClient - 주문 생성 테스트 에러 응답 본문이 비어있거나 JSON 형식이 아님 - status: {}, responseBody: {}",
                            e.status(), errorBody);
                }
                // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
                throw e;
            } catch (FeignException e) {
                log.error("업비트 주문 생성 테스트 실패 - status: {}, 실제 주문 생성을 중단합니다", e.status(), e);
                // 테스트 실패 시 예외를 그대로 전파
                throw e;
            }

            // 테스트 성공 후 실제 주문 생성 API 호출
            // 중요: 업비트는 각 요청마다 고유한 nonce를 요구하므로, 실제 주문 생성 시 새로운 JWT를 생성해야 함
            log.info("업비트 실제 주문 생성 JWT 생성 시작 - 새로운 nonce 사용");
            String actualOrderAuthorization = jwtApiUtil.createUpBitJwt(phoneNumber, null, request);

            UpbitResDTO.CreateOrder response = upbitFeignClient.createOrder(actualOrderAuthorization, request);

            log.info("업비트 주문 생성 완료 - uuid: {}, market: {}", response.uuid(), response.market());

            // 응답을 InvestResDTO.OrderDTO로 변환
            return new InvestResDTO.OrderDTO(
                    response.uuid(),
                    response.uuid(), // 업비트는 txid가 없으므로 uuid 사용
                    response.market(),
                    response.side(),
                    response.ord_type(),
                    parseCreatedAt(response.created_at())
            );

        } catch (MemberException e) {
            log.error("업비트 API 키를 찾을 수 없습니다 - phoneNumber: {}", phoneNumber, e);
            throw new InvestException(InvestErrorCode.API_KEY_NOT_FOUND);
        } catch (GeneralSecurityException e) {
            log.error("업비트 JWT 생성 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        } catch (FeignException.BadRequest | FeignException.NotFound e) {
            String errorBody = e.contentUTF8();

            // 응답 본문이 있고 JSON 형식인 경우에만 파싱 시도
            if (errorBody != null && !errorBody.isEmpty() && errorBody.trim().startsWith("{")) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ClientErrorDTO.Errors error = objectMapper.readValue(errorBody, ClientErrorDTO.Errors.class);
                    if (error != null && error.error() != null) {
                        log.error("UpbitApiClient - 주문 생성 에러 응답 - status: {}, errorName: {}, errorMessage: {}",
                                e.status(), error.error().name(), error.error().message());
                    } else {
                        log.error("UpbitApiClient - 주문 생성 에러 응답 - status: {}, responseBody: {}",
                                e.status(), errorBody);
                    }
                } catch (Exception parseException) {
                    // JSON 파싱 실패 시 로깅
                    log.error("UpbitApiClient - 주문 생성 에러 응답 파싱 실패 - status: {}, responseBody: {}, 파싱 에러: {}",
                            e.status(), errorBody, parseException.getMessage());
                }
            } else {
                // 응답 본문이 없거나 JSON이 아닌 경우
                log.warn("UpbitApiClient - 주문 생성 에러 응답 본문이 비어있거나 JSON 형식이 아님 - status: {}, responseBody: {}",
                        e.status(), errorBody);
            }
            // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
            throw e;
        } catch (FeignException.Unauthorized e) {
            String errorBody = e.contentUTF8();

            // 응답 본문이 있고 JSON 형식인 경우에만 파싱 시도
            if (errorBody != null && !errorBody.isEmpty() && errorBody.trim().startsWith("{")) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ClientErrorDTO.Errors error = objectMapper.readValue(errorBody, ClientErrorDTO.Errors.class);
                    if (error != null && error.error() != null) {
                        log.error("UpbitApiClient - 주문 생성 에러 응답 - status: {}, errorName: {}, errorMessage: {}",
                                e.status(), error.error().name(), error.error().message());
                    } else {
                        log.error("UpbitApiClient - 주문 생성 에러 응답 - status: {}, responseBody: {}",
                                e.status(), errorBody);
                    }
                } catch (Exception parseException) {
                    // JSON 파싱 실패 시 로깅
                    log.error("UpbitApiClient - 주문 생성 에러 응답 파싱 실패 - status: {}, responseBody: {}, 파싱 에러: {}",
                            e.status(), errorBody, parseException.getMessage());
                }
            } else {
                // 응답 본문이 없거나 JSON이 아닌 경우
                log.warn("UpbitApiClient - 주문 생성 에러 응답 본문이 비어있거나 JSON 형식이 아님 - status: {}, responseBody: {}",
                        e.status(), errorBody);
            }
            // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
            throw e;
        } catch (FeignException e) {
            // FeignException은 그대로 전파하여 상위에서 세부적인 분기 가능
            log.error("업비트 주문 생성 API 호출 실패 - FeignException: status: {}", e.status(), e);
            throw e;
        } catch (InvestException e) {
            throw e;
        } catch (Exception e) {
            log.error("업비트 주문 생성 API 호출 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }

    @Override
    public InvestResDTO.CancelOrderDTO cancelOrder(
            String phoneNumber,
            ExchangeType exchangeType,
            String uuid,
            String txid
    ) {
        try {
            log.info("업비트 주문 취소 API 호출 시작 - phoneNumber: {}, uuid: {}", phoneNumber, uuid);

            // 업비트는 query parameter로 uuid 전달
            String query = "uuid=" + uuid;
            String authorization = jwtApiUtil.createUpBitJwt(phoneNumber, query, null);

            log.info("업비트 주문 취소 인증 헤더 생성 완료");

            // 주문 취소 API 호출
            UpbitResDTO.CancelOrder response = upbitFeignClient.cancelOrder(authorization, uuid);

            // 응답을 InvestResDTO.CancelOrderDTO로 변환
            return new InvestResDTO.CancelOrderDTO(
                    response.uuid(),
                    response.uuid(), // 업비트는 txid가 없으므로 uuid 사용
                    parseCreatedAt(response.created_at())
            );

        } catch (MemberException e) {
            log.error("업비트 API 키를 찾을 수 없습니다 - phoneNumber: {}", phoneNumber, e);
            throw new InvestException(InvestErrorCode.API_KEY_NOT_FOUND);
        } catch (GeneralSecurityException e) {
            log.error("업비트 JWT 생성 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        } catch (FeignException.BadRequest e) {
            // 응답 본문에서 에러 코드 확인
            String errorBody = e.contentUTF8();
            if (errorBody != null && !errorBody.isEmpty() && errorBody.trim().startsWith("{")) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ClientErrorDTO.Errors errorResponse =
                            objectMapper.readValue(errorBody, ClientErrorDTO.Errors.class);

                    if (errorResponse != null && errorResponse.error() != null) {
                        String errorName = errorResponse.error().name();
                        String errorMessage = errorResponse.error().message();

                        if ("canceled_order".equals(errorName)) {
                            log.warn("업비트 주문 취소 - 이미 취소된 주문입니다. uuid: {}, message: {}", uuid, errorMessage);
                            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR,
                                    Map.of("error", "canceled_order", "message", "이미 취소된 주문입니다."));
                        } else if ("order_not_found".equals(errorName)) {
                            log.error("업비트 주문 취소 - 주문을 찾을 수 없습니다. uuid: {}, message: {}", uuid, errorMessage);
                            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR,
                                    Map.of("error", "order_not_found", "message", "주문을 찾을 수 없습니다."));
                        }
                    }
                } catch (InvestException investEx) {
                    throw investEx;
                } catch (Exception parseException) {
                    log.warn("업비트 주문 취소 에러 응답 파싱 실패: {}", parseException.getMessage());
                    // 파싱 실패 시 원본 FeignException 전파
                    throw e;
                }
            }
            // JSON이 아니거나 특정 에러가 아닌 경우 원본 FeignException 전파
            throw e;
        } catch (FeignException e) {
            // 다른 FeignException 타입들(Unauthorized, Forbidden 등)은 그대로 전파
            log.error("업비트 주문 취소 API 호출 실패 - FeignException: status: {}", e.status(), e);
            throw e;
        } catch (InvestException e) {
            throw e;
        } catch (Exception e) {
            log.error("업비트 주문 취소 API 호출 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }

    private LocalDateTime parseCreatedAt(String createdAt) {
        if (createdAt == null || createdAt.isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            // ISO 8601 형식 파싱 (예: "2026-01-10T19:51:25+09:00" 또는 "2026-01-10T19:51:25")
            String cleaned = createdAt.replace("+09:00", "").replace("Z", "");
            if (cleaned.contains("T")) {
                // "2026-01-10T19:51:25" 형식
                return LocalDateTime.parse(cleaned, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            return LocalDateTime.now();
        } catch (Exception e) {
            log.warn("created_at 파싱 실패: {}, 현재 시간 사용", createdAt);
            return LocalDateTime.now();
        }
    }
    
    /**
     * 업비트 마켓별 최소 주문 금액 조회
     * 참고:
     * - 원화(KRW) 마켓: https://docs.upbit.com/kr/docs/krw-market-info - 5,000 KRW
     * - BTC 마켓: https://docs.upbit.com/kr/docs/btc-market-info - 0.00005 BTC
     * - USDT 마켓: https://docs.upbit.com/kr/docs/usdt-market-info - 0.5 USDT
     */
    private BigDecimal getUpbitMinimumOrderAmount(String market) {
        if (market == null || market.isEmpty()) {
            log.warn("market이 null이거나 비어있어 원화 마켓 기본값(5000) 사용");
            return new BigDecimal("5000");
        }
        
        String upperMarket = market.toUpperCase();
        
        if (upperMarket.startsWith("KRW-")) {
            // 원화 마켓: 5,000 KRW
            log.info("업비트 원화 마켓 최소 주문 금액: 5000 KRW");
            return new BigDecimal("5000");
        } else if (upperMarket.startsWith("BTC-")) {
            // BTC 마켓: 0.00005 BTC
            log.info("업비트 BTC 마켓 최소 주문 금액: 0.00005 BTC");
            return new BigDecimal("0.00005");
        } else if (upperMarket.startsWith("USDT-")) {
            // USDT 마켓: 0.5 USDT
            // 참고: https://docs.upbit.com/kr/docs/usdt-market-info
            log.info("업비트 USDT 마켓 최소 주문 금액: 0.5 USDT");
            return new BigDecimal("0.5");
        } else {
            // 알 수 없는 마켓 타입: 원화 마켓 기본값 사용
            log.warn("알 수 없는 업비트 마켓 타입: {}, 원화 마켓 기본값(5000) 사용", market);
            return new BigDecimal("5000");
        }
    }
}