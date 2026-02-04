package com.example.scoi.domain.charge.controller;

import com.example.scoi.domain.charge.dto.ChargeReqDTO;
import com.example.scoi.domain.charge.dto.ChargeResDTO;
import com.example.scoi.domain.charge.dto.BalanceResDTO;
import com.example.scoi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "충전 API")
public interface ChargeControllerDocs {

    @Operation(
            summary = "원화 충전 요청하기 API By 김주헌",
            description = "코인을 구매하기 위한 원화 충전을 요청합니다. 반드시 인증서 발급을 한 뒤 호출해주세요."
    )
    ApiResponse<ChargeResDTO.ChargeKrw> chargeKrw(@AuthenticationPrincipal String phoneNumber, @RequestBody ChargeReqDTO.ChargeKrw dto);

    @Operation(
            summary = "특정 주문 확인하기 API By 김주헌",
            description = "특정 주문을 UUID로 스냅샷 형태로 확인합니다. 주문 체결 알림은 웹소켓 이용해서 실시간 추적, 체결 되면 FCM 토큰으로 알림이 갑니다."
    )
    ApiResponse<String> getOrders(@AuthenticationPrincipal String phoneNumber, @RequestBody ChargeReqDTO.GetOrder dto);

    @Operation(
            summary = "보유 자산 조회 API By 강서현",
            description = "현재 보유 자산을 조회합니다. 모든 보유 자산(KRW, BTC, ETH 등)을 배열로 반환합니다."
    )
    ApiResponse<BalanceResDTO.BalanceListDTO> getBalances(
            @RequestParam(defaultValue = "Bithumb") String exchangeType,
            @AuthenticationPrincipal String phoneNumber
    );
}
