package com.example.scoi.domain.myWallet.converter;

import com.example.scoi.domain.myWallet.dto.MyWalletResDTO;
import com.example.scoi.domain.myWallet.dto.TopupClientDTO;
import com.example.scoi.domain.myWallet.enums.RemitType;
import com.example.scoi.global.client.dto.BithumbResDTO;
import com.example.scoi.global.client.dto.UpbitResDTO;

import java.util.Collections;
import java.util.List;

public class MyWalletConverter {

    // ==================== 빗썸 변환 ====================

    public static MyWalletResDTO.TransactionDTO fromBithumbDeposit(BithumbResDTO.GetDeposit deposit) {
        return MyWalletResDTO.TransactionDTO.builder()
                .type(RemitType.DEPOSIT)
                .uuid(deposit.uuid())
                .currency(deposit.currency())
                .state(deposit.state())
                .amount(deposit.amount())
                .fee(deposit.fee())
                .txid(deposit.txid())
                .createdAt(deposit.created_at())
                .doneAt(deposit.done_at())
                .transactionType(deposit.transaction_type())
                .build();
    }

    public static MyWalletResDTO.TransactionDTO fromBithumbWithdraw(BithumbResDTO.GetWithdraw withdraw) {
        return MyWalletResDTO.TransactionDTO.builder()
                .type(RemitType.WITHDRAW)
                .uuid(withdraw.uuid())
                .currency(withdraw.currency())
                .state(withdraw.state())
                .amount(withdraw.amount())
                .fee(withdraw.fee())
                .txid(withdraw.txid())
                .createdAt(withdraw.created_at())
                .doneAt(withdraw.done_at())
                .transactionType(withdraw.transaction_type())
                .build();
    }

    // ==================== 업비트 변환 ====================

    public static MyWalletResDTO.TransactionDTO fromUpbitDeposit(UpbitResDTO.GetDeposit deposit) {
        return MyWalletResDTO.TransactionDTO.builder()
                .type(RemitType.DEPOSIT)
                .uuid(deposit.uuid())
                .currency(deposit.currency())
                .state(deposit.state())
                .amount(deposit.amount())
                .fee(deposit.fee())
                .txid(deposit.txid())
                .createdAt(deposit.created_at())
                .doneAt(deposit.done_at())
                .transactionType(deposit.transaction_type())
                .build();
    }

    public static MyWalletResDTO.TransactionDTO fromUpbitWithdraw(UpbitResDTO.GetWithdraw withdraw) {
        return MyWalletResDTO.TransactionDTO.builder()
                .type(RemitType.WITHDRAW)
                .uuid(withdraw.uuid())
                .currency(withdraw.currency())
                .state(withdraw.state())
                .amount(withdraw.amount())
                .fee(withdraw.fee())
                .txid(withdraw.txid())
                .createdAt(withdraw.created_at())
                .doneAt(withdraw.done_at())
                .transactionType(withdraw.transaction_type())
                .build();
    }

    // ==================== 잔량 설정 ====================

    /**
     * 기존 TransactionDTO에 balance(잔량)를 설정한 새 인스턴스를 반환합니다.
     */
    public static MyWalletResDTO.TransactionDTO withBalance(MyWalletResDTO.TransactionDTO tx, String balance) {
        return MyWalletResDTO.TransactionDTO.builder()
                .type(tx.type())
                .uuid(tx.uuid())
                .currency(tx.currency())
                .state(tx.state())
                .amount(tx.amount())
                .fee(tx.fee())
                .txid(tx.txid())
                .createdAt(tx.createdAt())
                .doneAt(tx.doneAt())
                .transactionType(tx.transactionType())
                .balance(balance)
                .build();
    }

    // ==================== 빗썸 주문 변환 ====================

    public static MyWalletResDTO.TopupTransactionDTO fromBithumbOrder(TopupClientDTO.BithumbOrder order) {
        return MyWalletResDTO.TopupTransactionDTO.builder()
                .uuid(order.uuid())
                .market(order.market())
                .side(order.side())
                .state(order.state())
                .createdAt(order.createdAt())
                .volume(order.volume())
                .executedVolume(order.executedVolume())
                .build();
    }

    // ==================== 업비트 주문 변환 ====================

    public static MyWalletResDTO.TopupTransactionDTO fromUpbitOrder(TopupClientDTO.UpbitOrder order) {
        return MyWalletResDTO.TopupTransactionDTO.builder()
                .uuid(order.uuid())
                .market(order.market())
                .side(order.side())
                .state(order.state())
                .createdAt(order.createdAt())
                .volume(order.volume())
                .executedVolume(order.executedVolume())
                .build();
    }

    // ==================== 상세 조회 변환 ====================

    /**
     * 빗썸 개별 입금 -> RemitDetailDTO
     */
    public static MyWalletResDTO.RemitDetailDTO fromBithumbDepositDetail(BithumbResDTO.GetDeposit deposit) {
        return MyWalletResDTO.RemitDetailDTO.builder()
                .type(deposit.type())
                .uuid(deposit.uuid())
                .currency(deposit.currency())
                .netType(deposit.net_type())
                .txid(deposit.txid())
                .state(deposit.state())
                .createdAt(deposit.created_at())
                .doneAt(deposit.done_at())
                .amount(deposit.amount())
                .fee(deposit.fee())
                .transactionType(deposit.transaction_type())
                .build();
    }

    /**
     * 빗썸 개별 출금 -> RemitDetailDTO
     */
    public static MyWalletResDTO.RemitDetailDTO fromBithumbWithdrawDetail(BithumbResDTO.GetWithdraw withdraw) {
        return MyWalletResDTO.RemitDetailDTO.builder()
                .type(withdraw.type())
                .uuid(withdraw.uuid())
                .currency(withdraw.currency())
                .netType(withdraw.net_type())
                .txid(withdraw.txid())
                .state(withdraw.state())
                .createdAt(withdraw.created_at())
                .doneAt(withdraw.done_at())
                .amount(withdraw.amount())
                .fee(withdraw.fee())
                .transactionType(withdraw.transaction_type())
                .build();
    }

    /**
     * 업비트 개별 입금 -> RemitDetailDTO
     */
    public static MyWalletResDTO.RemitDetailDTO fromUpbitDepositDetail(UpbitResDTO.GetDeposit deposit) {
        return MyWalletResDTO.RemitDetailDTO.builder()
                .type(deposit.type())
                .uuid(deposit.uuid())
                .currency(deposit.currency())
                .netType(deposit.net_type())
                .txid(deposit.txid())
                .state(deposit.state())
                .createdAt(deposit.created_at())
                .doneAt(deposit.done_at())
                .amount(deposit.amount())
                .fee(deposit.fee())
                .transactionType(deposit.transaction_type())
                .build();
    }

    /**
     * 업비트 개별 출금 -> RemitDetailDTO
     */
    public static MyWalletResDTO.RemitDetailDTO fromUpbitWithdrawDetail(UpbitResDTO.GetWithdraw withdraw) {
        return MyWalletResDTO.RemitDetailDTO.builder()
                .type(withdraw.type())
                .uuid(withdraw.uuid())
                .currency(withdraw.currency())
                .netType(withdraw.net_type())
                .txid(withdraw.txid())
                .state(withdraw.state())
                .createdAt(withdraw.created_at())
                .doneAt(withdraw.done_at())
                .amount(withdraw.amount())
                .fee(withdraw.fee())
                .transactionType(withdraw.transaction_type())
                .build();
    }

    /**
     * 빗썸 개별 주문 -> TopupDetailDTO
     */
    public static MyWalletResDTO.TopupDetailDTO fromBithumbOrderDetail(BithumbResDTO.GetOrder order) {
        List<MyWalletResDTO.TradeDTO> trades = order.trades() != null
                ? order.trades().stream()
                    .map(t -> MyWalletResDTO.TradeDTO.builder()
                            .market(t.market())
                            .uuid(t.uuid())
                            .price(t.price())
                            .volume(t.volume())
                            .funds(t.funds())
                            .side(t.side())
                            .createdAt(t.created_at())
                            .build())
                    .toList()
                : Collections.emptyList();

        return MyWalletResDTO.TopupDetailDTO.builder()
                .uuid(order.uuid())
                .market(order.market())
                .side(order.side())
                .ordType(order.ord_type())
                .price(order.price())
                .state(order.state())
                .createdAt(order.created_at())
                .volume(order.volume())
                .remainingVolume(order.remaining_volume())
                .executedVolume(order.executed_volume())
                .reservedFee(order.reserved_fee())
                .remainingFee(order.remaining_fee())
                .paidFee(order.paid_fee())
                .locked(order.locked())
                .tradesCount(order.trades_count())
                .trades(trades)
                .build();
    }

    /**
     * 업비트 개별 주문 -> TopupDetailDTO
     */
    public static MyWalletResDTO.TopupDetailDTO fromUpbitOrderDetail(UpbitResDTO.GetOrder order) {
        List<MyWalletResDTO.TradeDTO> trades = order.trades() != null
                ? order.trades().stream()
                    .map(t -> MyWalletResDTO.TradeDTO.builder()
                            .market(t.market())
                            .uuid(t.uuid())
                            .price(t.price())
                            .volume(t.volume())
                            .funds(t.funds())
                            .side(t.side())
                            .createdAt(t.created_at())
                            .build())
                    .toList()
                : Collections.emptyList();

        return MyWalletResDTO.TopupDetailDTO.builder()
                .uuid(order.uuid())
                .market(order.market())
                .side(order.side())
                .ordType(order.ord_type())
                .price(order.price())
                .state(order.state())
                .createdAt(order.created_at())
                .volume(order.volume())
                .remainingVolume(order.remaining_volume())
                .executedVolume(order.executed_volume())
                .reservedFee(order.reserved_fee())
                .remainingFee(order.remaining_fee())
                .paidFee(order.paid_fee())
                .locked(order.locked())
                .tradesCount(order.trades_count())
                .trades(trades)
                .build();
    }
}
