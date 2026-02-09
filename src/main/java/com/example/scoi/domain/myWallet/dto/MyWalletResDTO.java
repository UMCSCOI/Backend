package com.example.scoi.domain.myWallet.dto;

import com.example.scoi.domain.myWallet.enums.RemitType;
import lombok.Builder;

import java.util.List;

public class MyWalletResDTO {

    /**
     * 개별 거래 내역 (입금/출금 통합)
     */
    @Builder
    public record TransactionDTO(
            RemitType type,              // DEPOSIT 또는 WITHDRAW
            String uuid,                 // 거래 고유 ID
            String currency,             // 화폐 코드 (USDT, USDC 등)
            String state,                // 거래 상태 (PROCESSING, ACCEPTED, CANCELLED 등)
            String amount,               // 금액
            String fee,                  // 수수료
            String txid,                 // 거래소 트랜잭션 ID
            String createdAt,            // 생성 시간
            String doneAt,               // 완료 시간
            String transactionType,      // 거래 유형 (default, internal 등)
            String balance               // 해당 통화의 거래 후 잔량
    ) {}

    /**
     * 거래 내역 목록 응답
     */
    @Builder
    public record TransactionListDTO(
            List<TransactionDTO> transactions,
            int totalCount               // 필터링 후 반환된 건수
    ) {}

    /**
     * 개별 충전 거래 내역 (주문)
     */
    @Builder
    public record TopupTransactionDTO(
            String uuid,                 // 주문 고유 ID (상세 조회용 메타데이터)
            String market,               // 마켓 코드 (KRW-USDT, KRW-USDC)
            String side,                 // 주문 방향 (bid: 충전, ask: 현금교환)
            String state,                // 주문 상태 (done, wait, cancel)
            String createdAt,            // 생성 시간
            String volume,               // 주문량
            String executedVolume        // 체결량
    ) {}

    /**
     * 충전 거래 내역 목록 응답
     */
    @Builder
    public record TopupTransactionListDTO(
            List<TopupTransactionDTO> transactions,
            int totalCount               // 필터링 후 반환된 건수
    ) {}

    // ==================== 상세 조회 ====================

    /**
     * 입출금 상세 조회 응답 (개별 입금/출금의 전체 필드)
     */
    @Builder
    public record RemitDetailDTO(
            String type,                 // 입출금 종류 (deposit, withdraw)
            String uuid,                 // 고유 ID
            String currency,             // 화폐 코드
            String netType,              // 네트워크 타입
            String txid,                 // 트랜잭션 ID
            String state,                // 상태
            String createdAt,            // 생성 시간
            String doneAt,               // 완료 시간
            String amount,               // 금액
            String fee,                  // 수수료
            String transactionType       // 거래 유형 (default, internal 등)
    ) {}

    /**
     * 충전 상세 조회 응답 (개별 주문의 전체 필드 + 체결 내역)
     */
    @Builder
    public record TopupDetailDTO(
            String uuid,                 // 주문 고유 ID
            String market,               // 마켓 코드 (KRW-USDT, KRW-USDC)
            String side,                 // 주문 방향 (bid/ask)
            String ordType,              // 주문 유형 (limit, price, market 등)
            String price,                // 주문 가격
            String state,                // 주문 상태
            String createdAt,            // 생성 시간
            String volume,               // 주문량
            String remainingVolume,      // 잔여 주문량
            String executedVolume,       // 체결량
            String reservedFee,          // 예약된 수수료
            String remainingFee,         // 잔여 수수료
            String paidFee,              // 지불된 수수료
            String locked,               // 묶인 금액
            String tradesCount,          // 체결 건수
            List<TradeDTO> trades        // 체결 내역
    ) {}

    /**
     * 개별 체결 내역
     */
    @Builder
    public record TradeDTO(
            String market,               // 마켓 코드
            String uuid,                 // 체결 고유 ID
            String price,                // 체결 가격
            String volume,               // 체결량
            String funds,                // 체결 금액
            String side,                 // 체결 방향
            String createdAt             // 체결 시간
    ) {}

    /**
     * 거래내역 상세 조회 통합 응답
     * category에 따라 remitDetail 또는 topupDetail 중 하나가 non-null
     */
    @Builder
    public record TransactionDetailDTO(
            String category,             // "REMIT" 또는 "TOPUP"
            RemitDetailDTO remitDetail,  // 입출금 상세 (category=REMIT일 때)
            TopupDetailDTO topupDetail   // 충전 상세 (category=TOPUP일 때)
    ) {}

    // ==================== 원화 자산 조회 ====================

    /**
     * 원화(KRW) 자산 조회 응답
     */
    @Builder
    public record KrwBalanceDTO(
            String currency,             // 화폐 코드 (KRW)
            String balance               // 주문 가능 금액
    ) {}

    // ==================== 원화 출금 ====================

    /**
     * 원화(KRW) 출금 요청 응답
     */
    @Builder
    public record WithdrawKrwDTO(
            String currency,             // 화폐 코드 (KRW)
            String uuid,                 // 출금 고유 ID
            String txid                  // 트랜잭션 ID
    ) {}
}
