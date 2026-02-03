package com.example.scoi.domain.invest.controller;

import com.example.scoi.domain.invest.dto.InvestReqDTO;
import com.example.scoi.domain.invest.dto.InvestResDTO;
import com.example.scoi.domain.invest.dto.MaxOrderInfoDTO;
import com.example.scoi.domain.invest.exception.code.InvestSuccessCode;
import com.example.scoi.domain.invest.service.InvestService;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.global.apiPayload.ApiResponse;
import com.example.scoi.domain.invest.dto.InvestReqDTO;
import com.example.scoi.domain.invest.dto.InvestResDTO;
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
@RequestMapping("/api")
@Tag(name = "투자", description = "주문 관련련 API")
public class InvestController {

    private final InvestService investService;

    @PostMapping("/orders")
    @Operation(summary = "코인 주문하기", description = "코인 주문을 생성합니다.")
    @SecurityRequirement(name = "JWT TOKEN")
    public ApiResponse<InvestResDTO.OrderDTO> createOrder(
            @RequestBody InvestReqDTO.OrderDTO request,
            /* 임시 파라미터: JWT 인증 필터/인터셉터 구현 전까지 사용
                            이후 이 파라미터를 제거 JWT 토큰에서 memberId를 추출하여 사용
             */
            @RequestParam Long memberId
    ) {
        // exchangeType String을 ExchangeType enum으로 변환
        ExchangeType exchangeType;
        try {
            exchangeType = ExchangeType.fromString(request.getExchangeType());
        } catch (IllegalArgumentException e) {
            throw new InvestException(InvestErrorCode.INVALID_EXCHANGE_TYPE);
        }

        // 주문 생성
        InvestResDTO.OrderDTO result = investService.createOrder(
                memberId,
                exchangeType,
                request.getMarket(),
                request.getSide(),
                request.getOrderType(),
                request.getPrice(),
                request.getVolume(),
                request.getPassword()
        );

        return ApiResponse.onSuccess(InvestSuccessCode.ORDER_SUCCESS, result);
    }
}


}