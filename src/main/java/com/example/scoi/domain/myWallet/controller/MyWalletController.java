package com.example.scoi.domain.myWallet.controller;

import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.myWallet.dto.MyWalletResDTO;
import com.example.scoi.domain.myWallet.enums.DetailCategory;
import com.example.scoi.domain.myWallet.enums.OrderState;
import com.example.scoi.domain.myWallet.enums.PeriodType;
import com.example.scoi.domain.myWallet.enums.RemitType;
import com.example.scoi.domain.myWallet.enums.TopupType;
import com.example.scoi.domain.myWallet.exception.MyWalletException;
import com.example.scoi.domain.myWallet.exception.code.MyWalletErrorCode;
import com.example.scoi.domain.myWallet.exception.code.MyWalletSuccessCode;
import com.example.scoi.domain.myWallet.service.MyWalletService;
import com.example.scoi.global.apiPayload.ApiResponse;
import com.example.scoi.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mywallet")
public class MyWalletController implements MyWalletControllerDocs {

    private final MyWalletService myWalletService;

    // 거래 내역 전체 조회 (입출금)
    @GetMapping("/transactions/remit")
    public ApiResponse<MyWalletResDTO.TransactionListDTO> getRemitTransactions(
            @RequestParam(defaultValue = "BITHUMB") ExchangeType exchangeType,
            @RequestParam(defaultValue = "ALL") RemitType type,
            @RequestParam(defaultValue = "ONE_MONTH") PeriodType period,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        validateOrderParam(order);
        int safeLimit = Math.max(1, Math.min(limit, 100));

        MyWalletResDTO.TransactionListDTO result = myWalletService.getRemitTransactions(
                user.getUsername(),
                exchangeType,
                type,
                period,
                order,
                safeLimit
        );

        return ApiResponse.onSuccess(MyWalletSuccessCode.REMIT_TRANSACTIONS_SUCCESS, result);
    }

    // 거래 내역 전체 조회 (충전)
    @GetMapping("/transactions/topups")
    public ApiResponse<MyWalletResDTO.TopupTransactionListDTO> getTopupTransactions(
            @RequestParam(defaultValue = "BITHUMB") ExchangeType exchangeType,
            @RequestParam(defaultValue = "ALL") TopupType type,
            @RequestParam(defaultValue = "DONE") OrderState state,
            @RequestParam(defaultValue = "THREE_MONTHS") PeriodType period,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        validateOrderParam(order);
        int safeLimit = Math.max(1, Math.min(limit, 100));

        MyWalletResDTO.TopupTransactionListDTO result = myWalletService.getTopupTransactions(
                user.getUsername(),
                exchangeType,
                type,
                state,
                period,
                order,
                safeLimit
        );

        return ApiResponse.onSuccess(MyWalletSuccessCode.TOPUP_TRANSACTIONS_SUCCESS, result);
    }

    // 거래내역 상세 조회 (입출금 + 충전 통합)
    @GetMapping("/transactions/detail")
    public ApiResponse<MyWalletResDTO.TransactionDetailDTO> getTransactionDetail(
            @RequestParam(defaultValue = "BITHUMB") ExchangeType exchangeType,
            @RequestParam DetailCategory category,
            @RequestParam(required = false) RemitType remitType,
            @RequestParam String uuid,
            @RequestParam(required = false) String currency,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        // uuid 필수 검증
        if (uuid == null || uuid.isBlank()) {
            throw new MyWalletException(MyWalletErrorCode.INVALID_UUID_PARAM);
        }

        // category=REMIT일 때 remitType 필수 검증
        if (category == DetailCategory.REMIT && (remitType == null || remitType == RemitType.ALL)) {
            throw new MyWalletException(MyWalletErrorCode.INVALID_DETAIL_PARAM);
        }

        MyWalletResDTO.TransactionDetailDTO result = myWalletService.getTransactionDetail(
                user.getUsername(),
                exchangeType,
                category,
                remitType,
                uuid,
                currency
        );

        return ApiResponse.onSuccess(MyWalletSuccessCode.TRANSACTION_DETAIL_SUCCESS, result);
    }

    /**
     * order 파라미터 검증 (desc 또는 asc만 허용)
     */
    private void validateOrderParam(String order) {
        if (!"desc".equalsIgnoreCase(order) && !"asc".equalsIgnoreCase(order)) {
            throw new MyWalletException(MyWalletErrorCode.INVALID_ORDER_PARAM);
        }
    }
}
