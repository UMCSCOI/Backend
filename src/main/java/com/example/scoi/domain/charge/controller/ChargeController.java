package com.example.scoi.domain.charge.controller;

import com.example.scoi.domain.charge.dto.BalanceResDTO;
import com.example.scoi.domain.charge.dto.ChargeReqDTO;
import com.example.scoi.domain.charge.dto.ChargeResDTO;
import com.example.scoi.domain.charge.exception.code.ChargeSuccessCode;
import com.example.scoi.domain.charge.service.ChargeService;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.global.apiPayload.ApiResponse;
import com.example.scoi.global.apiPayload.code.BaseSuccessCode;
import com.example.scoi.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChargeController implements ChargeControllerDocs{

    private final ChargeService chargeService;

    // 원화 충전하기
    @PostMapping("/deposits/krw")
    public ApiResponse<ChargeResDTO.ChargeKrw> chargeKrw(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody ChargeReqDTO.ChargeKrw dto
    ){
        BaseSuccessCode code = ChargeSuccessCode.OK;
        return ApiResponse.onSuccess(code, chargeService.chargeKrw(user.getUsername(),dto));
    }

    // 특정 주문 확인하기
    @PostMapping("/deposits")
    public ApiResponse<String> getOrders(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody ChargeReqDTO.GetOrder dto
    ){
        BaseSuccessCode code = ChargeSuccessCode.OK;
        return ApiResponse.onSuccess(code, chargeService.getOrders(user.getUsername(), dto));
    }

    //보유자산 조회하기
    @GetMapping("/balances")
    public ApiResponse<BalanceResDTO.BalanceListDTO> getBalances(
            @RequestParam(defaultValue = "Bithumb") String exchangeType,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        String phoneNumber = user.getUsername();
        log.info("자산 조회 API 호출 - exchangeType: {}, phoneNumber: {}", exchangeType, phoneNumber);

        // try-catch 제거 - ExceptionAdvice에서 자동 처리
        ExchangeType exchangeTypeEnum = ExchangeType.fromString(exchangeType);
        log.info("ExchangeType 변환 완료: {}", exchangeTypeEnum);

        // JWT에서 가져온 phoneNumber로 조회
        BalanceResDTO.BalanceListDTO result = chargeService.getBalancesByPhone(user.getUsername(), exchangeTypeEnum);
        log.info("자산 조회 완료 - balances count: {}", result.balances() != null ? result.balances().size() : 0);

        return ApiResponse.onSuccess(ChargeSuccessCode.OK, result);
    }

    // 입금 주소 확인하기
    @GetMapping("/deposits/address")
    public ApiResponse<String> getDepositAddress(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam ExchangeType exchangeType
    ){
        BaseSuccessCode code = ChargeSuccessCode.OK;
        return ApiResponse.onSuccess(code, chargeService.getDepositAddress(user.getUsername(), exchangeType));
    }

    // 입금 주소 생성하기
    @PostMapping("/deposits/address")
    public ApiResponse<List<String>> createDepositAddress(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody ChargeReqDTO.CreateDepositAddress dto
    ){
        BaseSuccessCode code = ChargeSuccessCode.OK;
        return ApiResponse.onSuccess(code, chargeService.createDepositAddress(user.getUsername(), dto));
    }
}
