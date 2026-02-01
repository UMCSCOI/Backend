package com.example.scoi.domain.invest.controller;

import com.example.scoi.domain.invest.dto.InvestReqDTO;
import com.example.scoi.domain.invest.dto.InvestResDTO;
import com.example.scoi.domain.invest.dto.MaxOrderInfoDTO;
import com.example.scoi.domain.invest.exception.code.InvestSuccessCode;
import com.example.scoi.domain.invest.service.InvestService;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.global.apiPayload.ApiResponse;
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
public class InvestController implements InvestControllerDocs {
    
    private final InvestService investService;

    @GetMapping("/orders/info")
    @Override
    public ApiResponse<MaxOrderInfoDTO> getMaxOrderInfo(
            @RequestParam ExchangeType exchangeType,
            @RequestParam String coinType,
            @RequestParam(required = false) String price,  // 가격 (선택적)
            @AuthenticationPrincipal String phoneNumber
    ) {
        // JWT에서 추출한 phoneNumber로 조회
        MaxOrderInfoDTO result = investService.getMaxOrderInfo(phoneNumber, exchangeType, coinType, price);
        
        return ApiResponse.onSuccess(InvestSuccessCode.MAX_ORDER_INFO_SUCCESS, result);
    }

    @PostMapping("/orders/test")
    @Override
    public ApiResponse<Void> checkOrderAvailability(
            @RequestBody InvestReqDTO.OrderDTO request,
            @AuthenticationPrincipal String phoneNumber
    ) {
        // 주문 가능 여부 확인
        investService.checkOrderAvailability(
                phoneNumber,
                request.getExchangeType(),
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