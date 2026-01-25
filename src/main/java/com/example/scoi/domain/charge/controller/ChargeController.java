package com.example.scoi.domain.charge.controller;

import com.example.scoi.domain.charge.dto.ChargeReqDTO;
import com.example.scoi.domain.charge.dto.ChargeResDTO;
import com.example.scoi.domain.charge.exception.code.ChargeSuccessCode;
import com.example.scoi.domain.charge.service.ChargeService;
import com.example.scoi.global.apiPayload.ApiResponse;
import com.example.scoi.global.apiPayload.code.BaseSuccessCode;
import com.example.scoi.global.auth.entity.AuthUser;
import com.example.scoi.global.client.UpbitClient;
import com.example.scoi.global.util.JwtApiUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.GeneralSecurityException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChargeController {

    private final ChargeService chargeService;
    private final JwtApiUtil jwtApiUtil;
    private final UpbitClient upbitClient;

    // 임시
    @GetMapping("/test")
    public ApiResponse<List<String>> test() throws GeneralSecurityException {
        String token = jwtApiUtil.createUpBitJwt("01012341234", null, null);
        String result = upbitClient.getOrders(token);
        return ApiResponse.onSuccess(ChargeSuccessCode.OK, List.of(result));
    }

    // 원화 충전하기 (2차 인증서를 개발 단계에선 못함)
    @PostMapping("/deposits/krw")
    public ApiResponse<ChargeResDTO.ChargeKrw> chargeKrw(
            @AuthenticationPrincipal AuthUser user,
            @RequestBody ChargeReqDTO.ChargeKrw dto
    ){
        BaseSuccessCode code = ChargeSuccessCode.OK;
        return ApiResponse.onSuccess(code, chargeService.chargeKrw(user,dto));
    }

    // 특정 주문 확인하기
    @PostMapping("/deposits")
    public ApiResponse<String> getOrders(
            @AuthenticationPrincipal AuthUser user,
            @RequestBody ChargeReqDTO.GetOrder dto
    ){
        BaseSuccessCode code = ChargeSuccessCode.OK;
        return ApiResponse.onSuccess(code, chargeService.getOrders(user, dto));
    }
}
