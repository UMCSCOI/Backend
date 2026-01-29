package com.example.scoi.global.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.List;

public class UpbitResDTO {

    // 원화 입금
    public record ChargeKrw(
            String type,
            String uuid,
            String currency,
            String txid,
            String status,
            LocalDateTime created_at,
            LocalDateTime done_at,
            Long amount,
            Long fee,
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
}
