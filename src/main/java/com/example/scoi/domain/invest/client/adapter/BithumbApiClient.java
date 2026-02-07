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
import com.example.scoi.global.client.dto.ClientErrorDTO;
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
                    log.error("BithumbApiClient - 최대 주문 개수 조회 에러 응답 파싱 실패 - status: {}, responseBody: {}, 파싱 에러: {}", 
                            e.status(), errorBody, parseException.getMessage());
                    throw e; // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
                }
            } else {
                // 응답 본문이 없거나 JSON이 아닌 경우 - 원본 FeignException을 그대로 던짐
                log.warn("BithumbApiClient - 최대 주문 개수 조회 에러 응답 본문이 비어있거나 JSON 형식이 아님 - status: {}, responseBody: {}", 
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

                    // 권한이 부족한 경우
                    if (error.error().name().equals("out_of_scope")) {
                        throw new InvestException(InvestErrorCode.INSUFFICIENT_API_PERMISSION);
                    }

                    // 나머지 JWT 관련 오류
                    throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
                } catch (Exception parseException) {
                    // JSON 파싱 실패 시 로깅하고 원본 FeignException을 그대로 던짐
                    log.error("BithumbApiClient - 최대 주문 개수 조회 에러 응답 파싱 실패 - status: {}, responseBody: {}, 파싱 에러: {}", 
                            e.status(), errorBody, parseException.getMessage());
                    throw e; // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
                }
            } else {
                // 응답 본문이 없거나 JSON이 아닌 경우 - 원본 FeignException을 그대로 던짐
                log.warn("BithumbApiClient - 최대 주문 개수 조회 에러 응답 본문이 비어있거나 JSON 형식이 아님 - status: {}, responseBody: {}", 
                        e.status(), errorBody);
                throw e; // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
            }
        } catch (FeignException e) {
            // FeignException은 그대로 전파하여 상위에서 세부적인 분기 가능
            log.error("빗썸 최대 주문 정보 조회 API 호출 실패 - FeignException: status: {}", e.status(), e);
            throw e;
        } catch (Exception e) {
            // FeignException이 아닌 경우에만 InvestException으로 변환
            log.error("빗썸 최대 주문 정보 조회 API 호출 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }
    
    /**
     * /v1/orders/chance API 응답에서 최대 주문 정보 파싱
     * price가 있으면 balance / price로 최대 주문 수량(maxQuantity)
     */
    private MaxOrderInfoDTO parseMaxOrderInfoFromOrderChance(BithumbResDTO.OrderChance orderChance, String price) {
        try {
            String balance = "0";
            
            // bid_account에서 balance 추출  
            if (orderChance.bid_account() != null) {
                BithumbResDTO.BidAccount bidAccount = orderChance.bid_account();
                if (bidAccount.balance() != null && !bidAccount.balance().isEmpty()) {
                    balance = bidAccount.balance();
                }
            }
            
            // price가 있으면 최대 주문 수량 계산 (balance / price)
            String maxQuantity = null;
            if (price != null && !price.isEmpty()) {
                try {
                    BigDecimal balanceDecimal = new BigDecimal(balance);
                    BigDecimal priceDecimal = new BigDecimal(price);
                    
                    if (priceDecimal.compareTo(BigDecimal.ZERO) > 0) {
                        maxQuantity = balanceDecimal.divide(priceDecimal, 8, RoundingMode.DOWN).toPlainString();
                        log.info("빗썸 최대 주문 수량 계산 - balance: {}, price: {}, maxQuantity: {}", balance, price, maxQuantity);
                    } else {
                        log.warn("가격이 0 이하입니다. 최대 주문 수량을 계산할 수 없습니다.");
                    }
                } catch (NumberFormatException e) {
                    log.warn("가격 형식이 올바르지 않습니다. 최대 주문 수량을 계산할 수 없습니다. price: {}", price);
                }
            }
            
            log.info("빗썸 최대 주문 정보 조회 완료 - balance: {}, maxQuantity: {}", balance, maxQuantity);
            
            return new MaxOrderInfoDTO(balance, maxQuantity);
                    
        } catch (Exception e) {
            log.error("빗썸 최대 주문 정보 조회 API 응답 파싱 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
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
            // 주문 가능 정보 조회 
            BithumbResDTO.OrderChance orderChance = getOrderChance(phoneNumber, market);
            
            // 주문 가능 여부 검증 
            validateOrderAvailability(market, side, orderType, price, volume, orderChance);
            
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
            BithumbResDTO.OrderChance orderChance = bithumbFeignClient.getOrderChance(authorization, convertedMarket);
            
            if (orderChance.bid_account() != null || orderChance.ask_account() != null) {
                return orderChance;
            }
            
            log.error("빗썸 API 응답 형식이 예상과 다름. 응답 본문: {}", orderChance);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
            
        } catch (GeneralSecurityException e) {
            log.error("빗썸 JWT 생성 실패", e);
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
                    log.error("BithumbApiClient - 주문 가능여부 조회 에러 응답 파싱 실패 - status: {}, responseBody: {}, 파싱 에러: {}", 
                            e.status(), errorBody, parseException.getMessage());
                    throw e; // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
                }
            } else {
                // 응답 본문이 없거나 JSON이 아닌 경우 - 원본 FeignException을 그대로 던짐
                log.warn("BithumbApiClient - 주문 가능여부 조회 에러 응답 본문이 비어있거나 JSON 형식이 아님 - status: {}, responseBody: {}", 
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

                    // 권한이 부족한 경우
                    if (error.error().name().equals("out_of_scope")) {
                        throw new InvestException(InvestErrorCode.INSUFFICIENT_API_PERMISSION);
                    }

                    // 나머지 JWT 관련 오류
                    throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
                } catch (Exception parseException) {
                    // JSON 파싱 실패 시 로깅하고 원본 FeignException을 그대로 던짐
                    log.error("BithumbApiClient - 주문 가능여부 조회 에러 응답 파싱 실패 - status: {}, responseBody: {}, 파싱 에러: {}", 
                            e.status(), errorBody, parseException.getMessage());
                    throw e; // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
                }
            } else {
                // 응답 본문이 없거나 JSON이 아닌 경우 - 원본 FeignException을 그대로 던짐
                log.warn("BithumbApiClient - 주문 가능여부 조회 에러 응답 본문이 비어있거나 JSON 형식이 아님 - status: {}, responseBody: {}", 
                        e.status(), errorBody);
                throw e; // 원본 FeignException을 그대로 던져서 상위에서 세부적인 분기 가능
            }
        } catch (FeignException e) {
            // FeignException은 그대로 전파하여 상위에서 세부적인 분기 가능
            log.error("빗썸 주문 가능 정보 조회 실패 - FeignException: status: {}", e.status(), e);
            throw e;
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
            BithumbReqDTO.CreateOrder request = 
                    BithumbReqDTO.CreateOrder.builder()
                            .market(convertedMarket)
                            .side(side)
                            .ord_type(orderType)
                            .price(price)
                            .volume(volume)
                            .build();

            // 주문 가능 정보 조회하여 최소 주문 금액 가져오기
            // getOrderChance에서 에러가 발생해도 주문 생성은 계속 진행
            BithumbResDTO.OrderChance orderChance = null;
            try {
                orderChance = getOrderChance(phoneNumber, convertedMarket);
                // TODO: 5000원 테스트를 위해 임시로 최소 주문 금액 검증 비활성화
        
                log.info("빗썸 최소 주문 금액 검증을 건너뜁니다. (5000원 테스트용)");
            } catch (InvestException e) {
                // INSUFFICIENT_API_PERMISSION 등 에러 발생 시 최소 주문 금액 검증 건너뛰기
                if (e.getCode() == InvestErrorCode.INSUFFICIENT_API_PERMISSION) {
                    log.warn("빗썸 주문 가능 정보 조회 실패 (권한 부족) - 최소 주문 금액 검증을 건너뜁니다. 에러: {}", e.getMessage());
                } else {
                    log.warn("빗썸 주문 가능 정보 조회 실패 - 최소 주문 금액 검증을 건너뜁니다. 에러: {}", e.getMessage());
                }
                // 주문 생성은 계속 진행
            } catch (Exception e) {
                log.warn("빗썸 주문 가능 정보 조회 실패 - 최소 주문 금액 검증을 건너뜁니다. 에러: {}", e.getMessage());
                // 주문 생성은 계속 진행
            }

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
            // FeignException은 그대로 전파하여 상위에서 세부적인 분기 가능
            log.error("빗썸 주문 생성 API 호출 실패 - FeignException: status: {}", e.status(), e);
            throw e;
        } catch (InvestException e) {
            throw e;
        } catch (Exception e) {
            log.error("빗썸 주문 생성 API 호출 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }

    /**
     * 빗썸 최소 주문 금액 검증
     * OrderChance API에서 가져온 min_total 값을 사용하여 검증합니다.
     * - 지정가 매수 (limit): price * volume >= min_total
     * - 시장가 매수 (price): price >= min_total
     */
    private void validateMinimumOrderAmount(
            String side, 
            String orderType, 
            String price, 
            String volume,
            BithumbResDTO.OrderChance orderChance
    ) {
        // 매수 주문만 검증 (매도는 수량 기준이므로 별도 검증 필요 시 추가)
        if (!"bid".equals(side)) {
            return;
        }

        if (price == null || price.isEmpty()) {
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }

        // OrderChance에서 최소 주문 금액 가져오기
        String minTotalStr = null;
        if (orderChance.bid() != null && orderChance.bid().min_total() != null) {
            minTotalStr = orderChance.bid().min_total();
            log.info("빗썸 최소 주문 금액 (bid.min_total): {}", minTotalStr);
        } else {
            log.warn("빗썸 OrderChance 응답에 bid.min_total이 없습니다. 검증을 건너뜁니다.");
            return; // min_total이 없으면 검증 건너뛰기
        }

        if (minTotalStr == null || minTotalStr.isEmpty()) {
            log.warn("빗썸 최소 주문 금액이 비어있습니다. 검증을 건너뜁니다.");
            return;
        }

        BigDecimal priceDecimal = new BigDecimal(price);
        BigDecimal minimumAmount = new BigDecimal(minTotalStr);
        BigDecimal orderAmount;

        if ("limit".equals(orderType)) {
            // 지정가 매수: price * volume
            if (volume == null || volume.isEmpty()) {
                throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
            }
            BigDecimal volumeDecimal = new BigDecimal(volume);
            orderAmount = priceDecimal.multiply(volumeDecimal);
        } else if ("price".equals(orderType)) {
            // 시장가 매수: price (총액)
            orderAmount = priceDecimal;
        } else {
            // 매도 주문 등은 검증하지 않음
            return;
        }

        if (orderAmount.compareTo(minimumAmount) < 0) {
            log.warn("빗썸 최소 주문 금액 미달 - 주문 금액: {}, 최소 금액: {}", orderAmount, minimumAmount);
            Map<String, String> errorDetails = Map.of(
                "orderAmount", orderAmount.toPlainString(),
                "minimumAmount", minimumAmount.toPlainString()
            );
            throw new InvestException(InvestErrorCode.MINIMUM_ORDER_AMOUNT, errorDetails);
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
            // FeignException은 그대로 전파하여 상위에서 세부적인 분기 가능
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