package com.example.scoi.domain.invest.controller;

import com.example.scoi.domain.invest.dto.InvestReqDTO;
import com.example.scoi.domain.invest.dto.InvestResDTO;
import com.example.scoi.domain.invest.dto.MaxOrderInfoDTO;
import com.example.scoi.domain.invest.exception.InvestException;
import com.example.scoi.domain.invest.exception.code.InvestErrorCode;
import com.example.scoi.domain.invest.exception.code.InvestSuccessCode;
import com.example.scoi.domain.invest.service.InvestService;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "투자", description = "투자 관련 API")
public class InvestController {
    
    private final InvestService investService;

    @GetMapping("/orders/info")
    @Operation(summary = "최대 주문 개수 조회 By 강서현", description = "주문 생성 전, 가능한 금액과 정보를 조회합니다.")
    @SecurityRequirement(name = "JWT TOKEN")
    public ApiResponse<MaxOrderInfoDTO> getMaxOrderInfo(
            @RequestParam String exchangeType,
            @RequestParam String coinType,
            @RequestParam(required = false) String price,  // 가격 (선택적)
            @AuthenticationPrincipal String phoneNumber
    ) {
        // exchangeType String을 ExchangeType enum으로 변환
        ExchangeType exchangeTypeEnum;
        try {
            exchangeTypeEnum = ExchangeType.fromString(exchangeType);
        } catch (IllegalArgumentException e) {
            throw new InvestException(InvestErrorCode.INVALID_EXCHANGE_TYPE);
        }
        
        // JWT에서 추출한 phoneNumber로 조회
        MaxOrderInfoDTO result = investService.getMaxOrderInfo(phoneNumber, exchangeTypeEnum, coinType, price);
        
        return ApiResponse.onSuccess(InvestSuccessCode.MAX_ORDER_INFO_SUCCESS, result);
    }

    @PostMapping("/orders/test")
    @Operation(summary = "주문 가능 여부 확인 By 강서현", description = "주문 직전 해당 주문이 가능한지 여부를 확인합니다.")
    @SecurityRequirement(name = "JWT TOKEN")
    public ApiResponse<Void> checkOrderAvailability(
            @RequestBody InvestReqDTO.OrderDTO request,
            @AuthenticationPrincipal String phoneNumber
    ) {
        // exchangeType String을 ExchangeType enum으로 변환
        ExchangeType exchangeType;
        try {
            exchangeType = ExchangeType.fromString(request.getExchangeType());
        } catch (IllegalArgumentException e) {
            throw new InvestException(InvestErrorCode.INVALID_EXCHANGE_TYPE);
        }
        
        // 주문 가능 여부 확인
        investService.checkOrderAvailability(
                phoneNumber,
                exchangeType,
                request.getMarket(),
                request.getSide(),
                request.getOrderType(),
                request.getPrice(),
                request.getVolume()
        );
        
        // 주문 가능한 경우 200 응답 반환 ( result는 null)
        return ApiResponse.onSuccess(InvestSuccessCode.ORDER_AVAILABLE);
    }


}