package com.example.scoi.global.client.dto;

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
}
