package com.example.scoi.domain.myWallet.controller;

import com.example.scoi.domain.myWallet.dto.MyWalletReqDTO;
import com.example.scoi.domain.myWallet.dto.MyWalletResDTO;
import com.example.scoi.global.apiPayload.ApiResponse;
import com.example.scoi.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "내 지갑 API", description = "거래 내역 조회, 자산 관리")
public interface WithdrawControllerDocs {

    @Operation(
            summary = "원화 출금 요청 API By 원종호",
            description = "거래소에 원화(KRW) 출금을 요청합니다. " +
                    "빗썸은 카카오(KAKAO) 2차 인증만 지원하며, " +
                    "업비트는 카카오(KAKAO), 네이버(NAVER), 하나(HANA) 2차 인증을 지원합니다. " +
                    "요청 본문에 거래소 타입, 출금 금액, 2차 인증 수단을 포함해야 합니다."
    )
    ApiResponse<MyWalletResDTO.WithdrawKrwDTO> withdrawKrw(
            @Valid @RequestBody MyWalletReqDTO.WithdrawKrwRequest dto,
            @AuthenticationPrincipal CustomUserDetails user
    );
}
