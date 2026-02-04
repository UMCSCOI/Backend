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
}
