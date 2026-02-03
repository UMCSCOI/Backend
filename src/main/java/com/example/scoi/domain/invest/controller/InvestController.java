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
@Tag(name = "투자", description = "주문 관련 API")
public class InvestController implements InvestControllerDocs {

    private final InvestService investService;

    @GetMapping("/orders/info")
    @Override
    public ApiResponse<MaxOrderInfoDTO> getMaxOrderInfo(
            @RequestParam ExchangeType exchangeType,
            @RequestParam String coinType,
            @RequestParam(required = false) String price,
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

    @PostMapping("/orders")
    @Operation(summary = "코인 주문하기", description = "코인 주문을 생성합니다.")
    @SecurityRequirement(name = "JWT TOKEN")
    public ApiResponse<InvestResDTO.OrderDTO> createOrder(
            @RequestBody InvestReqDTO.OrderDTO request,
            @RequestParam Long memberId
    ) {
        // 주문 생성
        InvestResDTO.OrderDTO result = investService.createOrder(
                memberId,
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
}
