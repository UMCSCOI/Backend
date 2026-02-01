package com.example.scoi.domain.charge.controller;

import com.example.scoi.domain.charge.dto.ChargeReqDTO;
import com.example.scoi.domain.charge.dto.ChargeResDTO;
import com.example.scoi.domain.charge.dto.BalanceResDTO;
import com.example.scoi.domain.charge.exception.code.ChargeSuccessCode;
import com.example.scoi.domain.charge.service.ChargeService;
import com.example.scoi.global.apiPayload.ApiResponse;
import com.example.scoi.global.apiPayload.code.BaseSuccessCode;

import com.example.scoi.domain.charge.exception.ChargeException;
import com.example.scoi.domain.charge.exception.code.ChargeErrorCode;
import com.example.scoi.domain.member.enums.ExchangeType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChargeController implements ChargeControllerDocs{

    private final ChargeService chargeService;

    // 원화 충전하기 (2차 인증서를 개발 단계에선 못함)
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

    //보유자산 조회하기
    @GetMapping("/balances")
    public ApiResponse<BalanceResDTO.BalanceDTO> getBalances(
            @RequestParam(defaultValue = "Bithumb") String exchangeType,
            @AuthenticationPrincipal String phoneNumber
    ) {
        // exchangeType String을 ExchangeType enum으로 변환
        ExchangeType exchangeTypeEnum;
        try {
            exchangeTypeEnum = ExchangeType.fromString(exchangeType);
        } catch (IllegalArgumentException e) {
            throw new ChargeException(ChargeErrorCode.WRONG_EXCHANGE_TYPE);
        }

        // JWT에서 가져온 phoneNumber로 조회
        BalanceResDTO.BalanceDTO result = chargeService.getBalancesByPhone(phoneNumber, exchangeTypeEnum);

        return ApiResponse.onSuccess(ChargeSuccessCode.OK, result);
    }
}
