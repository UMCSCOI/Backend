package com.example.scoi.domain.invest.controller;

import com.example.scoi.domain.invest.dto.InvestReqDTO;
import com.example.scoi.domain.invest.dto.MaxOrderInfoDTO;
import com.example.scoi.domain.invest.exception.code.InvestSuccessCode;
import com.example.scoi.domain.invest.service.InvestService;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.global.apiPayload.ApiResponse;
import com.example.scoi.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class InvestController implements InvestControllerDocs {
    
    private final InvestService investService;

    @GetMapping("/orders/info")
    public ApiResponse<MaxOrderInfoDTO> getMaxOrderInfo(
            @RequestParam ExchangeType exchangeType,
            @RequestParam String coinType,
            @RequestParam(required = false) String unitPrice,  // 단위 가격 (선택적)
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        // JWT에서 추출한 phoneNumber로 조회
        MaxOrderInfoDTO result = investService.getMaxOrderInfo(user.getUsername(), exchangeType, coinType, unitPrice);
        
        return ApiResponse.onSuccess(InvestSuccessCode.MAX_ORDER_INFO_SUCCESS, result);
    }

    @PostMapping("/orders/test")
    public ApiResponse<Void> checkOrderAvailability(
            @RequestBody InvestReqDTO.OrderDTO request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        // 주문 가능 여부 확인
        investService.checkOrderAvailability(
                user.getUsername(),
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