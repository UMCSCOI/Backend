package com.example.scoi.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 빗썸 API 응답 DTO
 * 역할:
 * - 빗썸 API의 원본 응답 형식을 그대로 받아오는 DTO
 * - 공식 문서의 모든 필드를 포함
 * - Converter에서 이 DTO를 ChargeResDTO.BalanceDTO로 변환
 */
public class BithumbResDTO {


    // 원화 입금
    public record ChargeKrw(
            String type,
            String uuid,
            String currency,
            String txid,
            String status,
            String created_at,
            String done_at,
            String amount,
            String fee,
            String transaction_type
    ){}

    // 개별 주문 조회
    public record GetOrder(
            String uuid,
            String side,
            String ord_type,
            String price,
            String state,
            String market,
            String created_at,
            String volume,
            String remaining_volume,
            String reserved_fee,
            String remaining_fee,
            String paid_fee,
            String locked,
            String executed_volume,
            String trades_count,
            List<Trades> trades
    ){}

    // 개별 주문 조회
    public record Trades(
            String market,
            String uuid,
            String price,
            String volume,
            String funds,
            String side,
            String created_at
    ){}

    // 출금(이체) 가능 정보
    public record WithdrawsChance(
            MemberLevel member_level,
            Currency currency,
            Account account,
            WithdrawLimit withdraw_limit
    ) {}
    public record MemberLevel(
            Integer security_level,          // 보안등급
            Integer fee_level,               // 수수료등급
            Boolean email_verified,          // 이메일 인증 여부
            Boolean identity_auth_verified,  // 실명 인증 여부
            Boolean bank_account_verified,   // 계좌 인증 여부
            Boolean two_factor_auth_verified,// 2FA 인증 여부
            Boolean locked,                  // 계정 보호 상태
            Boolean wallet_locked            // 출금 보호 상태
    ) {}
    public record Currency(
            String code,                     // 화폐 코드 (BTC, ETH 등)
            String withdraw_fee,             // 출금 수수료
            Boolean is_coin,                 // 코인 여부
            String wallet_state,             // 지갑 상태 (working 등)
            List<String> wallet_support      // 지원하는 입출금 정보 (deposit, withdraw 등)
    ) {}
    public record Account(
            String currency,                 // 화폐 코드
            String balance,                  // 주문 가능 금액/수량
            String locked,                   // 주문 중 묶인 금액
            String avg_buy_price,            // 평균 매수가
            Boolean avg_buy_price_modified,  // 평균 매수가 수정 여부
            String unit_currency             // 평단가 기준 화폐
    ) {}
    public record WithdrawLimit(
            String currency,                 // 화폐 코드
            String minimum,                  // 최소 출금 금액
            String onetime,                  // 1회 한도
            String daily,                    // 1일 한도
            String remaining_daily,          // 1일 잔여 한도
            String remaining_daily_krw,      // 통합 1일 잔여 한도 (KRW 환산)
            Integer fixed,                   // 소수점 자리수
            Boolean can_withdraw             // 출금 지원 여부
    ) {}

    // 이체 결과
    public record WithdrawResDTO(
            String type,                // 입출금 종류
            String uuid,                // 출금의 고유 아이디
            String currency,            // 화폐 영문 대문자 코드
            String netType,             // 출금 네트워크
            String txid,                // 출금의 트랜잭션 아이디
            String state,               // 출금 상태
            String createdAt,           // 출금 생성 시간 (DateString)
            String done_at,             // 출금 완료 시간 (DateString)
            String amount,              // 출금 금액/수량 (NumberString)
            String fee,                 // 출금 수수료 (NumberString)
            String krwAmount,           // 원화 환산 가격 (NumberString)
            String transactionType      // 출금 유형 (default 등)
    ) {}

    // 개별 입금 조회
    public record GetDeposit(
            String type,
            String uuid,
            String currency,
            String net_type,
            String txid,
            String state,
            String created_at,
            String done_at,
            String amount,
            String fee,
            String transaction_type
    ){}

    // 주문 가능 정보 조회
    @JsonIgnoreProperties(ignoreUnknown = true)// DTO에 정의되지 않은 필드들이 와도 무시해달라는 설정정 - 추후 추가될 필드들을 위해 필요
    public record OrderChance(
            String bid_fee,
            String ask_fee,
            String maker_bid_fee,
            String maker_ask_fee,
            Market market,
            Bid bid,
            Ask ask,
            BidAccount bid_account,
            AskAccount ask_account,
            Boolean avg_buy_price_modified,
            String unit_currency
    ){}

    // 주문 가능 정보 조회 - Market
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Market(
            String id,
            String name,
            List<String> order_types,  // deprecated
            List<String> order_sides,
            List<String> bid_types,
            List<String> ask_types
    ){}

    // 주문 가능 정보 조회 - Bid
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Bid(
            String currency,
            Double price_unit,  // deprecated
            String min_total
    ){}

    // 주문 가능 정보 조회 - Ask
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ask(
            String currency,
            Double price_unit,  // deprecated
            String min_total,
            String max_total,
            String state
    ){}

    // 주문 가능 정보 조회 - BidAccount
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BidAccount(
            String currency,
            String balance,
            String locked,
            String avg_buy_price,
            Boolean avg_buy_price_modified,
            String unit_currency
    ){}

    // 주문 가능 정보 조회 - AskAccount
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AskAccount(
            String currency,
            String balance,
            String locked,
            String avg_buy_price,
            Boolean avg_buy_price_modified,
            String unit_currency
    ){}

    /**
     * 빗썸 전체 계좌 조회 응답 (배열)
     * 공식 문서: https://apidocs.bithumb.com/reference/전체-계좌-조회
     * 엔드포인트: GET /v1/accounts
     * 공식 문서 응답 형식 (Response 200):
     * array of objects
     * [
     *   {
     *     "currency": "KRW",
     *     "balance": "1000000",
     *     "locked": "50000",
     *     "avg_buy_price": "0",
     *     "avg_buy_price_modified": true,
     *     "unit_currency": "KRW"
     *   },
     *   ...
     * ]
     */
    public record BalanceResponse(
            String currency,
            String balance,
            String locked,
            String avg_buy_price,
            Boolean avg_buy_price_modified,
            String unit_currency
    ) {}
}
