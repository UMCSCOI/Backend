package com.example.scoi.domain.myWallet.controller;

import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.myWallet.dto.MyWalletResDTO;
import com.example.scoi.domain.myWallet.enums.DetailCategory;
import com.example.scoi.domain.myWallet.enums.OrderState;
import com.example.scoi.domain.myWallet.enums.PeriodType;
import com.example.scoi.domain.myWallet.enums.RemitType;
import com.example.scoi.domain.myWallet.enums.TopupType;
import com.example.scoi.global.apiPayload.ApiResponse;
import com.example.scoi.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "내 지갑 API", description = "거래 내역 조회, 자산 관리")
public interface MyWalletControllerDocs {

    @Operation(
            summary = "거래 내역 전체 조회(입출금) API By 원종호",
            description = "거래소의 코인(USDT/USDC) 입금/출금 내역을 통합 조회합니다. " +
                    "기간, 거래 유형(입금/출금/전체), 정렬 순서, 조회 건수를 지정할 수 있습니다."
    )
    ApiResponse<MyWalletResDTO.TransactionListDTO> getRemitTransactions(
            @Parameter(description = "거래소 타입 (BITHUMB, UPBIT)", example = "BITHUMB")
            @RequestParam(defaultValue = "BITHUMB") ExchangeType exchangeType,

            @Parameter(description = "조회 유형 (ALL: 전체, DEPOSIT: 입금, WITHDRAW: 출금)", example = "ALL")
            @RequestParam(defaultValue = "ALL") RemitType type,

            @Parameter(description = "조회 기간 (TODAY, ONE_MONTH, THREE_MONTHS, SIX_MONTHS)", example = "ONE_MONTH")
            @RequestParam(defaultValue = "ONE_MONTH") PeriodType period,

            @Parameter(description = "정렬 순서 (desc: 최신순, asc: 과거순)", example = "desc")
            @RequestParam(defaultValue = "desc") String order,

            @Parameter(description = "조회 건수 (최대 100)", example = "20")
            @RequestParam(defaultValue = "20") int limit,

            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "거래 내역 전체 조회(충전) API By 원종호",
            description = "거래소의 스테이블코인(KRW-USDT/KRW-USDC) 주문(충전/현금교환) 내역을 조회합니다. " +
                    "기간, 거래 유형(충전/현금교환/전체), 주문 상태(완료/대기/취소), 정렬 순서, 조회 건수를 지정할 수 있습니다. " +
                    "업비트의 경우 종료 주문은 7일 단위 윈도우로 조회하며, 최대 28회 API 호출 제한이 적용됩니다."
    )
    ApiResponse<MyWalletResDTO.TopupTransactionListDTO> getTopupTransactions(
            @Parameter(description = "거래소 타입 (BITHUMB, UPBIT)", example = "BITHUMB")
            @RequestParam(defaultValue = "BITHUMB") ExchangeType exchangeType,

            @Parameter(description = "조회 유형 (ALL: 전체, CHARGE: 충전/매수, CASH_EXCHANGE: 현금교환/매도)", example = "ALL")
            @RequestParam(defaultValue = "ALL") TopupType type,

            @Parameter(description = "주문 상태 (DONE: 완료, WAIT: 대기, CANCEL: 취소)", example = "DONE")
            @RequestParam(defaultValue = "DONE") OrderState state,

            @Parameter(description = "조회 기간 (TODAY, ONE_MONTH, THREE_MONTHS, SIX_MONTHS)", example = "THREE_MONTHS")
            @RequestParam(defaultValue = "THREE_MONTHS") PeriodType period,

            @Parameter(description = "정렬 순서 (desc: 최신순, asc: 과거순)", example = "desc")
            @RequestParam(defaultValue = "desc") String order,

            @Parameter(description = "조회 건수 (최대 100)", example = "20")
            @RequestParam(defaultValue = "20") int limit,

            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "원화 자산 조회 API By 원종호",
            description = "거래소의 원화(KRW) 자산 잔고를 조회합니다. " +
                    "주문 가능 금액(balance)과 주문 중 묶인 금액(locked)을 반환합니다."
    )
    ApiResponse<MyWalletResDTO.KrwBalanceDTO> getKrwBalance(
            @Parameter(description = "거래소 타입 (BITHUMB, UPBIT)", example = "BITHUMB")
            @RequestParam(defaultValue = "BITHUMB") ExchangeType exchangeType,

            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "거래내역 상세 조회 API By 원종호",
            description = "입출금 또는 충전 거래의 상세 내역을 UUID로 조회합니다. " +
                    "category=REMIT이면 입금/출금 상세, category=TOPUP이면 주문 상세(체결 내역 포함)를 반환합니다. " +
                    "입출금 조회 시 remitType(DEPOSIT/WITHDRAW) 파라미터가 필수입니다."
    )
    ApiResponse<MyWalletResDTO.TransactionDetailDTO> getTransactionDetail(
            @Parameter(description = "거래소 타입 (BITHUMB, UPBIT)", example = "BITHUMB")
            @RequestParam(defaultValue = "BITHUMB") ExchangeType exchangeType,

            @Parameter(description = "조회 카테고리 (REMIT: 입출금, TOPUP: 충전)", example = "REMIT")
            @RequestParam DetailCategory category,

            @Parameter(description = "입출금 구분 (DEPOSIT: 입금, WITHDRAW: 출금) - category=REMIT일 때 필수", example = "DEPOSIT")
            @RequestParam(required = false) RemitType remitType,

            @Parameter(description = "거래 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam String uuid,

            @Parameter(description = "통화 코드 (USDT, USDC 등) - category=REMIT일 때 선택", example = "USDT")
            @RequestParam(required = false) String currency,

            @AuthenticationPrincipal CustomUserDetails user
    );
}
