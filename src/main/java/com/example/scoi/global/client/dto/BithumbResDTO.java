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
