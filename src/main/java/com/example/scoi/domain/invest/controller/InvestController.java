package com.example.scoi.domain.invest.controller;

import com.example.scoi.domain.invest.dto.InvestReqDTO;
import com.example.scoi.domain.invest.dto.InvestResDTO;
import com.example.scoi.domain.invest.dto.MaxOrderInfoDTO;
import com.example.scoi.domain.invest.exception.code.InvestSuccessCode;
import com.example.scoi.domain.invest.service.InvestService;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.global.apiPayload.ApiResponse;
import com.example.scoi.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.example.scoi.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(required = false) String unitPrice,  // 단위 가격 (선택적)
            @RequestParam(required = false) String orderType,    // 주문 타입 (limit, price, market)
            @RequestParam(required = false) String side,        // 주문 방향 (bid, ask)
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        // JWT에서 추출한 phoneNumber로 조회
        MaxOrderInfoDTO result = investService.getMaxOrderInfo(user.getUsername(), exchangeType, coinType, unitPrice, orderType, side);
        
        return ApiResponse.onSuccess(InvestSuccessCode.MAX_ORDER_INFO_SUCCESS, result);
    }

    @PostMapping("/orders/test")
    @Override
    public ApiResponse<Void> checkOrderAvailability(
            @RequestBody InvestReqDTO.OrderDTO request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        String phoneNumber = user.getUsername();
        // 주문 가능 여부 확인
        investService.checkOrderAvailability(
                phoneNumber,
                request.exchangeType(),
                request.market(),
                request.side(),
                request.orderType(),
                request.price(),
                request.volume()
        );

        // 주문 가능한 경우 200 응답 반환 (result는 null)
        return ApiResponse.onSuccess(InvestSuccessCode.ORDER_AVAILABLE);
    }

    @PostMapping("/orders/test-create")
    @Override
    @SecurityRequirement(name = "JWT TOKEN")
    public ApiResponse<InvestResDTO.OrderDTO> testCreateOrder(
            @RequestBody InvestReqDTO.TestOrderDTO request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        String phoneNumber = user.getUsername();

        // 주문 생성 테스트 (password 불필요)
        InvestResDTO.OrderDTO result = investService.testCreateOrder(
                phoneNumber,
                request.exchangeType(),
                request.market(),
                request.side(),
                request.orderType(),
                request.price(),
                request.volume()
        );

        return ApiResponse.onSuccess(InvestSuccessCode.ORDER_SUCCESS, result);
    }

    @PostMapping("/orders")
    @Override
    @SecurityRequirement(name = "JWT TOKEN")
    public ApiResponse<InvestResDTO.OrderDTO> createOrder(
            @RequestBody InvestReqDTO.OrderDTO request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        String phoneNumber = user.getUsername();

        // 주문 생성
        InvestResDTO.OrderDTO result = investService.createOrder(
                phoneNumber,
                request.exchangeType(),
                request.market(),
                request.side(),
                request.orderType(),
                request.price(),
                request.volume(),
                request.password()
        );

        return ApiResponse.onSuccess(InvestSuccessCode.ORDER_SUCCESS, result);
    }

    @DeleteMapping("/orders")
    @Override
    @SecurityRequirement(name = "JWT TOKEN")
    public ApiResponse<InvestResDTO.CancelOrderDTO> cancelOrder(
            @RequestBody InvestReqDTO.CancelOrderDTO request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        String phoneNumber = user.getUsername();

        InvestResDTO.CancelOrderDTO result = investService.cancelOrder(
                phoneNumber,
                request.exchangeType(),
                request.uuid(),
                request.txid()
        );

        return ApiResponse.onSuccess(InvestSuccessCode.ORDER_CANCEL_SUCCESS, result);
    }
}
