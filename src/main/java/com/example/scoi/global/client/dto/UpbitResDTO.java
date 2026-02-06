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
