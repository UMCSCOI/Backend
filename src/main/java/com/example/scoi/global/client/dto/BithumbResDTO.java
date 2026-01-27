package com.example.scoi.global.client.dto;

import java.time.LocalDateTime;
import java.util.List;

public class BithumbResDTO {


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
}
