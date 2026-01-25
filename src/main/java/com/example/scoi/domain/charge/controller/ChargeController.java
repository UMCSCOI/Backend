package com.example.scoi.domain.charge.controller;


import com.example.scoi.domain.charge.dto.ChargeResDTO;
import com.example.scoi.domain.charge.exception.ChargeException;
import com.example.scoi.domain.charge.exception.code.ChargeErrorCode;
import com.example.scoi.domain.charge.exception.code.ChargeSuccessCode;
import com.example.scoi.domain.charge.service.ChargeService;
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
@Tag(name = "충전", description = "충전 관련 API")
public class ChargeController {
 private final ChargeService chargeService;

    @GetMapping("/balances")
    @Operation(summary = "보유 자산 조회", description = "현재 보유 자산을 조회합니다.")
    @SecurityRequirement(name = "JWT TOKEN")
    public ApiResponse<ChargeResDTO.BalanceDTO> getBalances(
            @RequestParam(defaultValue = "Bithumb") String exchangeType,
            /* TODO: JWT 인증 필터/인터셉터 구현 후
             * - @RequestParam 대신 JWT에서 memberId 추출
             * - 또는 AuthUser 파라미터 사용 (팀원 패턴 참고)
             * 
             * 현재는 정상 버전: memberId를 Query Parameter로 받아서 Member 조회
             */
            @RequestParam Long memberId
    ) {
        // exchangeType String을 ExchangeType enum으로 변환
        ExchangeType exchangeTypeEnum;
        try {
            exchangeTypeEnum = ExchangeType.fromString(exchangeType);
        } catch (IllegalArgumentException e) {
            throw new ChargeException(ChargeErrorCode.INVALID_EXCHANGE_TYPE);
        }
        
        // 정상 버전: Member 조회 후 phoneNumber 사용
        ChargeResDTO.BalanceDTO result = chargeService.getBalances(memberId, exchangeTypeEnum);
        
        return ApiResponse.onSuccess(ChargeSuccessCode.BALANCE_INQUIRY_SUCCESS, result);
    }

}
