package com.example.scoi.domain.charge.controller;

import com.example.scoi.domain.charge.dto.ChargeReqDTO;
import com.example.scoi.domain.charge.dto.ChargeResDTO;
import com.example.scoi.domain.charge.exception.code.ChargeSuccessCode;
import com.example.scoi.domain.charge.service.ChargeService;
import com.example.scoi.global.apiPayload.ApiResponse;
import com.example.scoi.global.apiPayload.code.BaseSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChargeController implements ChargeControllerDocs{

    private final ChargeService chargeService;

    // 원화 충전하기
    @PostMapping("/deposits/krw")
    public ApiResponse<ChargeResDTO.ChargeKrw> chargeKrw(
            @AuthenticationPrincipal String phoneNumber,
            @RequestBody ChargeReqDTO.ChargeKrw dto
    ){
        BaseSuccessCode code = ChargeSuccessCode.OK;
        return ApiResponse.onSuccess(code, chargeService.chargeKrw(phoneNumber,dto));
    }

    // 특정 주문 확인하기
    @PostMapping("/deposits")
    public ApiResponse<String> getOrders(
            @AuthenticationPrincipal String phoneNumber,
            @RequestBody ChargeReqDTO.GetOrder dto
    ){
        BaseSuccessCode code = ChargeSuccessCode.OK;
        return ApiResponse.onSuccess(code, chargeService.getOrders(phoneNumber, dto));
    }
}
