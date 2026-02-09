package com.example.scoi.domain.myWallet.controller;

import com.example.scoi.domain.myWallet.dto.MyWalletReqDTO;
import com.example.scoi.domain.myWallet.dto.MyWalletResDTO;
import com.example.scoi.domain.myWallet.exception.code.MyWalletSuccessCode;
import com.example.scoi.domain.myWallet.service.MyWalletService;
import com.example.scoi.global.apiPayload.ApiResponse;
import com.example.scoi.global.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class WithdrawController implements WithdrawControllerDocs {

    private final MyWalletService myWalletService;

    // 원화 출금 요청
    @PostMapping("/withdraws/krw")
    public ApiResponse<MyWalletResDTO.WithdrawKrwDTO> withdrawKrw(
            @Valid @RequestBody MyWalletReqDTO.WithdrawKrwRequest dto,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        MyWalletResDTO.WithdrawKrwDTO result = myWalletService.withdrawKrw(
                user.getUsername(),
                dto
        );

        return ApiResponse.onSuccess(MyWalletSuccessCode.WITHDRAW_KRW_SUCCESS, result);
    }
}
