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
            
            // price가 있으면 최대 주문 수량 계산 (balance / price)
            String maxQuantity = null;
            if (price != null && !price.isEmpty()) {
                try {
                    BigDecimal balanceDecimal = new BigDecimal(balance);
                    BigDecimal priceDecimal = new BigDecimal(price);
                    
                    if (priceDecimal.compareTo(BigDecimal.ZERO) > 0) {
                        maxQuantity = balanceDecimal.divide(priceDecimal, 8, RoundingMode.DOWN).toPlainString();
                    }
                } catch (NumberFormatException e) {
                    log.warn("가격 형식이 올바르지 않습니다. 최대 주문 수량을 계산할 수 없습니다. price: {}", price);
                }
            }
            
            return new MaxOrderInfoDTO(balance, maxQuantity);
                    
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

                    // 권한이 부족한 경우
                    if (error.error().name().equals("out_of_scope")) {
                        throw new InvestException(InvestErrorCode.INSUFFICIENT_API_PERMISSION);
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
}