package com.example.scoi.domain.myWallet.exception.code;

import com.example.scoi.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MyWalletSuccessCode implements BaseSuccessCode {

    REMIT_TRANSACTIONS_SUCCESS(HttpStatus.OK,
            "MYWALLET200_1",
            "거래 내역을 성공적으로 조회했습니다."),
    TOPUP_TRANSACTIONS_SUCCESS(HttpStatus.OK,
            "MYWALLET200_2",
            "충전 거래 내역을 성공적으로 조회했습니다."),
    TRANSACTION_DETAIL_SUCCESS(HttpStatus.OK,
            "MYWALLET200_3",
            "거래 상세 내역을 성공적으로 조회했습니다."),
    KRW_BALANCE_SUCCESS(HttpStatus.OK,
            "MYWALLET200_4",
            "원화 자산을 성공적으로 조회했습니다."),
    WITHDRAW_KRW_SUCCESS(HttpStatus.OK,
            "MYWALLET200_5",
            "원화 출금을 성공적으로 요청했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
