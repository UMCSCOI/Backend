package com.example.scoi.global.client.dto;

import java.util.List;

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
}
