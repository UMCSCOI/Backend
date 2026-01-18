package com.example.scoi.domain.invest.client;

import com.example.scoi.domain.invest.dto.InvestResDTO;
import com.example.scoi.domain.invest.exception.InvestException;
import com.example.scoi.domain.invest.exception.code.InvestErrorCode;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.global.util.JwtApiUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpbitApiClient implements ExchangeApiClient {
    
    private final RestTemplate restTemplate;
    private final JwtApiUtil jwtApiUtil;
    private final ObjectMapper objectMapper;
    
    @Override
    public InvestResDTO.MaxOrderInfoDTO getMaxOrderInfo(String phoneNumber, ExchangeType exchangeType, String coinType) {
        try {
            // JWT 토큰 생성 (Bearer 포함)
            String authorization = jwtApiUtil.createUpBitJwt(phoneNumber, null, null);
            log.info("업비트 최대 주문 정보 조회 API 호출 시작 - phoneNumber: {}, coinType: {}", phoneNumber, coinType);
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authorization);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 업비트 API 호출 (계정 잔고 조회)
            ResponseEntity<String> response = restTemplate.exchange(
                "https://api.upbit.com/v1/accounts",
                HttpMethod.GET,
                entity,
                String.class
            );
            
            log.info("업비트 최대 주문 정보 조회 API 응답 상태: {}", response.getStatusCode());
            log.debug("업비트 최대 주문 정보 조회 API 응답 본문: {}", response.getBody());
            
            // 응답 파싱 및 변환
            return parseMaxOrderInfoResponse(response.getBody(), coinType);
            
        } catch (GeneralSecurityException e) {
            log.error("업비트 JWT 생성 실패", e);
            throw new RuntimeException("JWT 생성 실패", e);
        } catch (Exception e) {
            log.error("업비트 최대 주문 정보 조회 API 호출 실패", e);
            throw new RuntimeException("업비트 API 호출 실패: " + e.getMessage(), e);
        }
    }
    
    private InvestResDTO.MaxOrderInfoDTO parseMaxOrderInfoResponse(String responseBody, String coinType) {
        try {
            // 업비트 API 응답 형식에 맞게 파싱 -문서 확인 필요요
            
            List<Map<String, Object>> accounts = objectMapper.readValue(
                responseBody,
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            // coinType에 해당하는 계좌 찾기
            // coinType 형식: KRW-BTC -> currency: BTC
            String currency = coinType.contains("-") ? coinType.split("-")[1] : coinType;
            
            String balance = "0";
            for (Map<String, Object> account : accounts) {
                String accountCurrency = String.valueOf(account.get("currency"));
                if (currency.equals(accountCurrency)) {
                    // available 필드가 있으면 사용, 없으면 balance 사용
                    if (account.containsKey("available")) {
                        balance = String.valueOf(account.get("available"));
                    } else if (account.containsKey("balance")) {
                        balance = String.valueOf(account.get("balance"));
                    }
                    break;
                }
            }
            
            // KRW인 경우 KRW 계좌 찾기
            if ("KRW".equals(currency)) {
                for (Map<String, Object> account : accounts) {
                    if ("KRW".equals(account.get("currency"))) {
                        balance = String.valueOf(account.get("balance"));
                        break;
                    }
                }
            }
            
            log.info("업비트 최대 주문 정보 조회 완료 - coinType: {}, balance: {}", coinType, balance);
            
            return InvestResDTO.MaxOrderInfoDTO.builder()
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
            
            // 1. 주문 가능 정보 조회 (GET /v1/orders/chance)
            // 참고: https://docs.upbit.com/kr/reference/available-order-information
            Map<String, Object> orderChance = getOrderChance(phoneNumber, market);
            
            // 2. 주문 가능 여부 검증
            validateOrderAvailability(market, side, orderType, price, volume, orderChance);
            
            log.info("업비트 주문 가능 여부 확인 완료 - 주문 가능");
            
        } catch (InvestException e) {
            throw e;
        } catch (Exception e) {
            log.error("업비트 주문 가능 여부 확인 API 호출 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }
    
    /**
     * 주문 가능 정보 조회 (GET /v1/orders/chance)
     * 참고: https://docs.upbit.com/kr/reference/available-order-information
     */
    private Map<String, Object> getOrderChance(String phoneNumber, String market) {
        try {
            // JWT 토큰 생성 (query parameter 포함)
            String query = "market=" + market;
            String authorization = jwtApiUtil.createUpBitJwt(phoneNumber, query, null);
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authorization);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 업비트 API 호출 (주문 가능 정보 조회)
            // 참고: https://docs.upbit.com/kr/reference/available-order-information
            String url = "https://api.upbit.com/v1/orders/chance?" + query;
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            // 응답 파싱
            Map<String, Object> orderChance = objectMapper.readValue(
                response.getBody(),
                new TypeReference<Map<String, Object>>() {}
            );
            
            return orderChance;
            
        } catch (GeneralSecurityException e) {
            log.error("업비트 JWT 생성 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        } catch (Exception e) {
            log.error("업비트 주문 가능 정보 조회 실패", e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }
    
    /**
     * 주문 가능 여부 검증
     * GET /v1/orders/chance 응답에서 bid_account, ask_account의 balance 사용
     */
    private void validateOrderAvailability(
            String market,
            String side,
            String orderType,
            String price,
            String volume,
            Map<String, Object> orderChance
    ) {
        // orderChance에서 계좌 잔고 추출
        String balance;
        if ("bid".equals(side)) {
            // 매수: bid_account.balance 사용
            Map<String, Object> bidAccount = (Map<String, Object>) orderChance.get("bid_account");
            if (bidAccount != null && bidAccount.containsKey("balance")) {
                balance = String.valueOf(bidAccount.get("balance"));
            } else {
                log.error("업비트 주문 가능 정보 응답에 bid_account.balance가 없습니다.");
                throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
            }
        } else if ("ask".equals(side)) {
            // 매도: ask_account.balance 사용
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
            // 매수: 계좌 잔고 확인
            BigDecimal requiredAmount;
            
            if ("limit".equals(orderType)) {
                // 지정가 매수: price * volume
                requiredAmount = priceDecimal.multiply(volumeDecimal);
            } else if ("price".equals(orderType)) {
                // 시장가 매수: price가 총액
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
    
    /**
     * market과 side에서 통화 추출
     */
    private String extractCurrency(String market, String side) {
        if (market.contains("-")) {
            String[] parts = market.split("-");
            if ("bid".equals(side)) {
                return parts[0]; // KRW
            } else {
                return parts[1]; // BTC
            }
        }
        return market;
    }
}