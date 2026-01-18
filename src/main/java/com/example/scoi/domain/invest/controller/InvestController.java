package com.example.scoi.domain.invest.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
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
    @Operation(summary = "최대 주문 개수 조회", description = "주문 생성 전, 가능한 금액과 정보를 조회합니다.")
    @SecurityRequirement(name = "JWT TOKEN")
    public ApiResponse<MaxOrderInfoDTO> getMaxOrderInfo(
            @RequestParam String exchangeType,
            @RequestParam String coinType,
            /* 임시 파라미터: JWT 인증 필터/인터셉터 구현 전까지 사용
                            이후 이 파라미터를 제거 JWT 토큰에서 memberId를 추출하여 사용
              @RequestParam 대신 다른 어노테이션 사용
             => Charge  Controller 와 동일!!
             현재는 로컬 테스트를 위해 Query Parameter로 받고 있음
             */
            @RequestParam Long memberId
    ) {
        // exchangeType String을 ExchangeType enum으로 변환
        ExchangeType exchangeTypeEnum;
        try {
            exchangeTypeEnum = ExchangeType.fromString(exchangeType);
        } catch (IllegalArgumentException e) {
            throw new InvestException(InvestErrorCode.INVALID_EXCHANGE_TYPE);
        }
        
        // 정상 버전: Member 조회 후 phoneNumber 사용
        MaxOrderInfoDTO result = investService.getMaxOrderInfo(memberId, exchangeTypeEnum, coinType);
        
        return ApiResponse.onSuccess(InvestSuccessCode.MAX_ORDER_INFO_SUCCESS, result);
    }

    @PostMapping("/orders/test")
    @Operation(summary = "주문 가능 여부 확인", description = "주문 직전 해당 주문이 가능한지 여부를 확인합니다.")
    @SecurityRequirement(name = "JWT TOKEN")
    public ApiResponse<Void> checkOrderAvailability(
            @RequestBody InvestReqDTO.OrderTestDTO request,
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
        
        // 주문 가능 여부 확인
        investService.checkOrderAvailability(
                memberId,
                exchangeType,
                request.getMarket(),
                request.getSide(),
                request.getOrderType(),
                request.getPrice(),
                request.getVolume()
        );
        
        return ApiResponse.onSuccess(InvestSuccessCode.ORDER_AVAILABLE);
    }


}