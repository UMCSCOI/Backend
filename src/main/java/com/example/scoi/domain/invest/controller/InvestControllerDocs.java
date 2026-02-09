package com.example.scoi.domain.invest.controller;

import com.example.scoi.domain.invest.dto.InvestReqDTO;
import com.example.scoi.domain.invest.dto.InvestResDTO;
import com.example.scoi.domain.invest.dto.MaxOrderInfoDTO;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.global.apiPayload.ApiResponse;
import com.example.scoi.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "투자 API", description = "최대 개수 조회,주문 가능 여부 확인, 코인 주문/취소")
public interface InvestControllerDocs {

    @Operation(
            summary = "최대 주문 개수 조회 By 강서현",
            description = "주문 생성 전, 가능한 금액과 정보를 조회합니다."
    )
    ApiResponse<MaxOrderInfoDTO> getMaxOrderInfo(
            @RequestParam ExchangeType exchangeType,
            @RequestParam String coinType,
            @RequestParam(required = false) String unitPrice,
            @RequestParam(required = false) String orderType,
            @RequestParam(required = false) String side,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "주문 가능 여부 확인 By 강서현",
            description = "주문 직전 해당 주문이 가능한지 여부를 확인합니다."
    )
    ApiResponse<Void> checkOrderAvailability(
            @RequestBody InvestReqDTO.OrderDTO request,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "주문 생성 테스트 By 강서현",
            description = "실제 주문을 생성하지 않고 주문 요청 형식과 주문 가능 여부를 검증합니다. " +
                         "업비트 API의 주문 생성 테스트 엔드포인트(/v1/orders/test)를 사용하여 거래 수수료 없이 검증할 수 있습니다. " +
                         "password는 필요하지 않습니다."
    )
    ApiResponse<InvestResDTO.OrderDTO> testCreateOrder(
            @RequestBody InvestReqDTO.TestOrderDTO request,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "코인 주문하기  By 강서현",
            description = "코인 주문을 생성합니다."
    )
    ApiResponse<InvestResDTO.OrderDTO> createOrder(
            @RequestBody InvestReqDTO.OrderDTO request,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "주문 취소  By 강서현",
            description = "생성한 주문을 취소합니다."
    )
    ApiResponse<InvestResDTO.CancelOrderDTO> cancelOrder(
            @RequestBody InvestReqDTO.CancelOrderDTO request,
            @AuthenticationPrincipal CustomUserDetails user
    );
}
