package com.example.scoi.domain.invest.client.adapter;

import com.example.scoi.domain.invest.client.ExchangeApiClient;
import com.example.scoi.domain.invest.client.feign.BithumbFeignClient;
import com.example.scoi.domain.invest.dto.InvestResDTO;
import com.example.scoi.domain.invest.dto.MaxOrderInfoDTO;
import com.example.scoi.domain.invest.exception.InvestException;
import com.example.scoi.domain.invest.exception.code.InvestErrorCode;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.global.client.dto.BithumbReqDTO;
import com.example.scoi.global.client.dto.BithumbResDTO;
import com.example.scoi.global.util.JwtApiUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import feign.FeignException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class BithumbApiClient implements ExchangeApiClient {
    
    private final BithumbFeignClient bithumbFeignClient;
    private final JwtApiUtil jwtApiUtil;
    
    @Override
    public MaxOrderInfoDTO getMaxOrderInfo(String phoneNumber, ExchangeType exchangeType, String coinType, String unitPrice, String orderType, String side) {
        try {
            // coinType에서 실제 코인 추출 (KRW-USDC -> USDC, USDC -> USDC, KRW -> KRW)
            String targetCoin = extractCoinFromCoinType(coinType);
            
            String authorization = jwtApiUtil.createBithumbJwt(phoneNumber, null, null);
            log.info("빗썸 최대 주문 정보 조회 API 호출 시작 - phoneNumber: {}, coinType: {} (대상 코인: {}), unitPrice: {}, orderType: {}, side: {}", 
                    phoneNumber, coinType, targetCoin, unitPrice, orderType, side);
            
            // 전체 계좌 조회
            BithumbResDTO.BalanceResponse[] accounts = bithumbFeignClient.getAccounts(authorization);
            List<BithumbResDTO.BalanceResponse> accountList = Arrays.asList(accounts);
            
            log.info("빗썸 최대 주문 정보 조회 API 응답 수신 - 계좌 개수: {}", accountList.size());
            // 디버깅: 실제 응답 값 확인
            for (BithumbResDTO.BalanceResponse account : accountList) {
                log.info("계좌 정보 - currency: {}, balance: {}, locked: {}", 
                        account.currency(), account.balance(), account.locked());
            }
            
            return parseMaxOrderInfoResponse(accountList, targetCoin, coinType, unitPrice, orderType, side);
            
        } catch (GeneralSecurityException e) {
            log.error("빗썸 JWT 생성 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        } catch (FeignException e) {
            log.error("빗썸 최대 주문 정보 조회 API 호출 실패 (FeignException)", e);
            throw e;
        } catch (Exception e) {
            log.error("빗썸 최대 주문 정보 조회 API 호출 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }
    
    /**
     * /v1/accounts API 응답에서 최대 주문 정보 파싱
     * targetCoin에 해당하는 잔고를 조회
     * unitPrice가 있으면 balance / unitPrice로 최대 주문 수량(maxQuantity)
     * 시장가 주문인 경우: 시장가 매수는 KRW 잔액, 시장가 매도는 코인 잔액을 maxQuantity로 반환
     */
    private MaxOrderInfoDTO parseMaxOrderInfoResponse(List<BithumbResDTO.BalanceResponse> accounts, String targetCoin, String coinType, String unitPrice, String orderType, String side) {
        try {
            String balance = "0";
            String targetCurrency;
            
            // 시장가 주문인 경우
            boolean isMarketOrder = "price".equals(orderType) || "market".equals(orderType);
            
            if (isMarketOrder) {
                // side 파라미터를 기준으로 매수/매도 판단
                if ("bid".equals(side)) {
                    // 매수: KRW 잔액 조회
                    targetCurrency = "KRW";
                    log.info("빗썸 시장가 매수 - {}를 매수하기 위해 KRW 잔액 조회", targetCoin);
                } else if ("ask".equals(side)) {
                    // 매도: 코인 잔액 조회
                    targetCurrency = targetCoin;
                    log.info("빗썸 시장가 매도 - {} 잔액 조회", targetCoin);
                } else {
                    // side가 없거나 잘못된 경우 기본값 (매수)
                    targetCurrency = "KRW";
                    log.warn("빗썸 시장가 주문 - side 파라미터가 없거나 잘못됨 ({}), 기본값으로 매수 처리", side);
                }
            } else {
                // 지정가 주문: side 파라미터를 기준으로 판단
                if ("bid".equals(side)) {
                    // 지정가 매수: KRW 잔액 조회
                    targetCurrency = "KRW";
                    log.info("빗썸 지정가 매수 - {}를 매수하기 위해 KRW 잔액 조회", targetCoin);
                } else if ("ask".equals(side)) {
                    // 지정가 매도: 코인 잔액 조회
                    targetCurrency = targetCoin;
                    log.info("빗썸 지정가 매도 - {} 잔액 조회", targetCoin);
                } else {
                    // side가 없거나 잘못된 경우 기본값 (매수)
                    targetCurrency = "KRW";
                    log.warn("빗썸 지정가 주문 - side 파라미터가 없거나 잘못됨 ({}), 기본값으로 매수 처리", side);
                }
            }
            
            // 해당 currency로 계좌 찾기
            for (BithumbResDTO.BalanceResponse account : accounts) {
                if (targetCurrency.equals(account.currency())) {
                    log.info("해당 currency 계좌 발견 - currency: {} (조회 목적: {}), balance: {}, locked: {}", 
                            account.currency(), targetCoin, account.balance(), account.locked());
                    
                    // balance - locked로 사용 가능 잔고 계산
                    if (account.balance() != null && !account.balance().isEmpty()) {
                        try {
                            BigDecimal balanceDecimal = new BigDecimal(account.balance());
                            BigDecimal lockedDecimal = account.locked() != null && !account.locked().isEmpty() 
                                    ? new BigDecimal(account.locked()) 
                                    : BigDecimal.ZERO;
                            BigDecimal availableDecimal = balanceDecimal.subtract(lockedDecimal);
                            balance = availableDecimal.toPlainString();
                            log.info("사용 가능 잔고 계산 - balance: {}, locked: {}, available: {}", 
                                    account.balance(), account.locked(), balance);
                        } catch (NumberFormatException e) {
                            // 계산 실패 시 balance 그대로 사용
                            balance = account.balance();
                            log.warn("사용 가능 잔고 계산 실패, balance 사용: {}", balance);
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
                        String market = convertMarketForBithumb(coinType);
                        log.info("빗썸 현재가 조회 시작 - market: {} (coinType: {}, targetCoin: {})", market, coinType, targetCoin);
                        String tickerResponse = bithumbFeignClient.getTicker(market);
                        
                        if (tickerResponse != null && !tickerResponse.isEmpty()) {
                            ObjectMapper objectMapper = new ObjectMapper();
                            BithumbResDTO.Ticker ticker = null;
                            
                            // 빗썸 API는 배열을 반환할 수 있으므로 배열로 파싱 시도
                            try {
                                BithumbResDTO.Ticker[] tickers = objectMapper.readValue(tickerResponse, BithumbResDTO.Ticker[].class);
                                if (tickers != null && tickers.length > 0) {
                                    ticker = tickers[0];
                                    log.debug("빗썸 현재가 조회 - 배열 형식으로 파싱 성공");
                                }
                            } catch (Exception arrayException) {
                                // 배열 파싱 실패 시 단일 객체로 파싱 시도
                                try {
                                    ticker = objectMapper.readValue(tickerResponse, BithumbResDTO.Ticker.class);
                                    log.debug("빗썸 현재가 조회 - 단일 객체 형식으로 파싱 성공");
                                } catch (Exception singleException) {
                                    log.error("빗썸 현재가 조회 - JSON 파싱 실패 (배열/단일 객체 모두 실패): {}", singleException.getMessage());
                                    throw singleException;
                                }
                            }
                            
                            if (ticker != null && ticker.trade_price() != null && ticker.trade_price() > 0) {
                                BigDecimal balanceDecimal = new BigDecimal(balance);
                                BigDecimal currentPrice = BigDecimal.valueOf(ticker.trade_price());
                                BigDecimal quantity = balanceDecimal.divide(currentPrice, 8, RoundingMode.DOWN);
                                // 소수점 절사하여 정수로 변환
                                maxQuantity = quantity.setScale(0, RoundingMode.DOWN).toPlainString();
                                log.info("빗썸 시장가 매수 - KRW 잔액: {}, 현재가: {}, 최대 매수 가능 수량: {} (정수)", 
                                        balance, currentPrice, maxQuantity);
                            } else {
                                log.warn("빗썸 현재가 조회 실패 또는 가격이 0 이하 - market: {}, ticker: {}", market, ticker);
                                maxQuantity = null;
                            }
                        } else {
                            log.warn("빗썸 현재가 조회 실패 - 응답이 비어있음 - market: {}", market);
                            maxQuantity = null;
                        }
                    } catch (Exception e) {
                        log.error("빗썸 현재가 조회 실패 - 시장가 매수 수량 계산 불가: {}", e.getMessage(), e);
                        maxQuantity = null;
                    }
                } else if ("ask".equals(side)) {
                    // 시장가 매도: 코인 잔액을 maxQuantity로 반환 (최대 매도 가능 수량)
                    // 소수점 절사하여 정수로 변환
                    try {
                        BigDecimal balanceDecimal = new BigDecimal(balance);
                        maxQuantity = balanceDecimal.setScale(0, RoundingMode.DOWN).toPlainString();
                        log.info("빗썸 시장가 매도 - 코인 잔액: {}, 최대 매도 가능 수량: {} (정수)", balance, maxQuantity);
                    } catch (NumberFormatException e) {
                        log.warn("빗썸 시장가 매도 - 잔액 파싱 실패, 원본 값 사용: {}", balance);
                        maxQuantity = balance;
                    }
                } else {
                    log.warn("빗썸 시장가 주문 - side 파라미터가 없거나 잘못됨 ({}), maxQuantity: null", side);
                    maxQuantity = null;
                }
            } else {
                // 지정가 주문
                if (unitPrice != null && !unitPrice.isEmpty()) {
                    try {
                        BigDecimal balanceDecimal = new BigDecimal(balance);
                        BigDecimal unitPriceDecimal = new BigDecimal(unitPrice);
                        
                        if (unitPriceDecimal.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal quantity = balanceDecimal.divide(unitPriceDecimal, 8, RoundingMode.DOWN);
                            // 소수점 절사하여 정수로 변환
                            maxQuantity = quantity.setScale(0, RoundingMode.DOWN).toPlainString();
                            log.info("빗썸 최대 주문 수량 계산 - balance: {}, unitPrice: {}, maxQuantity: {} (정수)", 
                                    balance, unitPrice, maxQuantity);
                        } else {
                            log.warn("단위 가격이 0 이하입니다. 최대 주문 수량을 계산할 수 없습니다.");
                        }
                    } catch (NumberFormatException e) {
                        log.warn("단위 가격 형식이 올바르지 않습니다. 최대 주문 수량을 계산할 수 없습니다. unitPrice: {}", unitPrice);
                    }
                }
            }
            
            log.info("빗썸 최대 주문 정보 조회 완료 - targetCoin: {}, balance: {}, maxQuantity: {}, orderType: {}", 
                    targetCoin, balance, maxQuantity, orderType);
            
            return new MaxOrderInfoDTO(balance, maxQuantity);
            
        } catch (Exception e) {
            log.error("빗썸 최대 주문 정보 조회 API 응답 파싱 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }
    
    /**
     * coinType에서 실제 코인 추출
     * KRW-USDC -> USDC
     * USDC -> USDC
     * KRW -> KRW
     * BTC -> BTC
     */
    private String extractCoinFromCoinType(String coinType) {
        if (coinType == null || coinType.isEmpty()) {
            return "KRW";
        }
        
        // KRW-XXX 형식인 경우
        if (coinType.contains("-") && coinType.startsWith("KRW-")) {
            return coinType.substring(4); // "KRW-" 제거
        }
        
        // XXX-KRW 형식인 경우
        if (coinType.contains("-")) {
            String[] parts = coinType.split("-");
            if (parts.length == 2 && "KRW".equals(parts[1])) {
                return parts[0]; // BTC-KRW -> BTC
            }
        }
        
        // 언더스코어 형식: BTC_KRW -> BTC
        if (coinType.contains("_")) {
            String[] parts = coinType.split("_");
            if (parts.length == 2) {
                return parts[0]; // BTC_KRW -> BTC
            }
        }
        
        // 그 외의 경우 그대로 반환 (USDC, BTC, KRW 등)
        return coinType;
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
        
        // 코인 타입만 있는 경우 (예: USDC, BTC) -> KRW-XXX 형식으로 변환
        // 빗썸 API는 KRW-USDC 형식을 사용
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
            
            // API 응답에서 실제 마켓 형식 확인
            if (orderChance.market() != null && orderChance.market().id() != null) {
                log.info("빗썸 API 응답 market.id: {} (요청한 market: {})", orderChance.market().id(), convertedMarket);
            }
            
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
        
        // 코인 타입만 있는 경우 (예: USDC, BTC) -> KRW-XXX 형식으로 변환
        // 빗썸 API는 KRW-USDC 형식을 사용
        return "KRW-" + market;
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
        // 빗썸은 주문 생성 테스트 엔드포인트를 지원하지 않음
        throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
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
            log.info("빗썸 주문 생성 API 호출 시작 - phoneNumber: {}, market: {}, side: {}, orderType: {}",
                    phoneNumber, market, side, orderType);

            // 마켓 형식 변환
            String convertedMarket = convertMarketForBithumb(market);

            // 주문 생성 요청 DTO 생성
            BithumbReqDTO.CreateOrder request = BithumbReqDTO.CreateOrder.builder()
                    .market(convertedMarket)
                    .side(side)
                    .ord_type(orderType)
                    .price(price)
                    .volume(volume)
                    .build();

            // JWT 생성 (POST 요청이므로 body 사용)
            String authorization = jwtApiUtil.createBithumbJwt(phoneNumber, null, request);

            // 주문 생성 API 호출
            BithumbResDTO.CreateOrder response = bithumbFeignClient.createOrder(authorization, request);

            log.info("빗썸 주문 생성 완료 - uuid: {}, market: {}", response.uuid(), response.market());

            // 응답을 InvestResDTO.OrderDTO로 변환
            return new InvestResDTO.OrderDTO(
                    response.uuid(),
                    response.uuid(), // 빗썸은 txid가 없으므로 uuid 사용
                    response.market(),
                    response.side(),
                    response.ord_type(),
                    parseCreatedAt(response.created_at())
            );

        } catch (GeneralSecurityException e) {
            log.error("빗썸 JWT 생성 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        } catch (FeignException e) {
            log.error("빗썸 주문 생성 API 호출 실패 - FeignException: status: {}", e.status(), e);
            throw e;
        } catch (InvestException e) {
            throw e;
        } catch (Exception e) {
            log.error("빗썸 주문 생성 API 호출 실패", e);
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
            log.info("빗썸 주문 취소 API 호출 시작 - phoneNumber: {}, uuid: {}", phoneNumber, uuid);

            // 빗썸은 query parameter로 uuid 전달
            String query = "uuid=" + uuid;
            String authorization = jwtApiUtil.createBithumbJwt(phoneNumber, query, null);

            log.info("빗썸 주문 취소 인증 헤더 생성 완료");

            // 주문 취소 API 호출
            BithumbResDTO.CancelOrder response = bithumbFeignClient.cancelOrder(authorization, uuid);

            log.info("빗썸 주문 취소 완료 - uuid: {}", response.uuid());

            // 응답을 InvestResDTO.CancelOrderDTO로 변환
            return new InvestResDTO.CancelOrderDTO(
                    response.uuid(),
                    response.uuid(), // 빗썸은 txid가 없으므로 uuid 사용
                    parseCreatedAt(response.created_at())
            );

        } catch (GeneralSecurityException e) {
            log.error("빗썸 JWT 생성 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        } catch (FeignException e) {
            log.error("빗썸 주문 취소 API 호출 실패 - FeignException: status: {}", e.status(), e);
            throw e;
        } catch (InvestException e) {
            throw e;
        } catch (Exception e) {
            log.error("빗썸 주문 취소 API 호출 실패", e);
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
}