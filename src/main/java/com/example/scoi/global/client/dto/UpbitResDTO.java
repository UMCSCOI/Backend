package com.example.scoi.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 업비트 API 응답 DTO
 * 역할:
 * - 업비트 API의 원본 응답 형식을 그대로 받아오는 DTO
 * - 공식 문서의 모든 필드를 포함
 * - Converter에서 이 DTO를 ChargeResDTO.BalanceDTO로 변환
 */
public class UpbitResDTO {

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
            String market,
            String uuid,
            String side,
            String ord_type,
            String price,
            String state,
            String created_at,
            String volume,
            String remaining_volume,
            String executed_volume,
            String reserved_fee,
            String remaining_fee,
            String paid_fee,
            String locked,
            String time_in_force,
            String smp_type,
            String prevented_volume,
            String prevented_locked,
            String identifier,
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
            String trend,
            String created_at,
            String side
    ){}

    // 출금(이체) 가능 정보
    public record WithdrawsChance(
            MemberLevel member_level,          // 사용자 보안 등급 정보
            Currency currency,                  // 화폐 정보
            Account account,                    // 계좌 잔액 정보
            WithdrawLimit withdraw_limit        // 출금 제약 정보
    ) {
        public record MemberLevel(
                String security_level,
                String fee_level,
                Boolean email_verified,
                Boolean identity_auth_verified,
                Boolean bank_account_verified,
                Boolean two_factor_auth_verified,
                Boolean locked,
                Boolean wallet_locked
        ) {}
        public record Currency(
                String code,
                String withdraw_fee,            // 출금 수수료
                Boolean is_coin,
                String wallet_state             // 지갑 상태 (working, etc.)
        ) {}
        public record Account(
                String currency,
                String balance,                 // 주문 가능 금액
                String locked,                  // 주문 중 묶인 금액
                String avg_buy_price
        ) {}
        public record WithdrawLimit(
                String currency,
                String minimum,                 // 최소 출금 금액
                String onetime,                // 1회 출금 한도
                String daily,                  // 1일 출금 한도
                String remaining_daily,        // 1일 잔여 출금 한도
                Boolean can_withdraw           // 출금 지원 여부
        ) {}
    }

    //이체 결과
    public record WithdrawResDTO(
            String type,                // 입출금 종류 (기본값: withdraw)
            String uuid,                // 출금의 유일 식별자
            String currency,            // 통화 코드 (예: BTC)
            String netType,             // 출금 네트워크 유형
            String txid,                // 트랜잭션 ID (실패/대기 시 null 가능)
            String state,               // 출금 상태 (WAITING, DONE 등)
            String createdAt,           // 출금 생성 시간
            String doneAt,              // 출금 완료 시간 (미완료 시 null)
            String amount,              // 출금 수량
            String fee,                 // 출금 수수료
            String transactionType,     // 출금 유형 (default: 일반, internal: 바로출금)
            Boolean isCancelable        // 출금 취소 가능 여부
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
    @JsonIgnoreProperties(ignoreUnknown = true) // DTO에 정의되지 않은 필드들이 와도 무시해달라는 설정정 - 추후 추가될 필드들을 위해 필요요

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

    // 전체 계좌 조회
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Account(
            String currency,
            String balance,
            String locked,
            String avg_buy_price,
            Boolean avg_buy_price_modified,
            String unit_currency,
            String available  // 매수 가능 금액/수량
    ){}

    // 개별 입금 주소 조회
    public record GetDepositAddress(
            String currency,
            String net_type,
            String deposit_address,
            String secondary_address
    ){}

    // 입금 주소 생성 요청
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CreateDepositAddress(
            String success, // 201 생성 요청 직후
            String message, // 201 생성 요청 직후
            String currency, // 200 생성 이후
            String net_type, // 200 생성 이후
            String deposit_address, // 200 생성 이후
            String secondary_address // 200 생성 이후
    ){}

    /**
     * 업비트 계정 잔고 조회 응답 (배열)
     * 공식 문서: https://docs.upbit.com/kr/reference/get-balance
     * 엔드포인트: GET /v1/accounts
     * 공식 문서의 모든 필드를 포함:
     * - currency: 화폐를 의미하는 영문 대문자 코드
     * - balance: 주문가능 금액/수량
     * - locked: 주문 중 묶여있는 금액/수량
     * - avg_buy_price: 매수평균가
     * - avg_buy_price_modified: 매수평균가 수정 여부
     * - unit_currency: 평단가 기준 화폐
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceResponse {

        private String currency;  // 화폐를 의미하는 영문 대문자 코드

        private String balance;  // 주문가능 금액/수량

        private String locked;  // 주문 중 묶여있는 금액/수량

        @JsonProperty("avg_buy_price")
        private String avgBuyPrice;  // 매수평균가

        @JsonProperty("avg_buy_price_modified")
        private Boolean avgBuyPriceModified;  // 매수평균가 수정 여부

        @JsonProperty("unit_currency")
        private String unitCurrency;  // 평단가 기준 화폐
    }
}
