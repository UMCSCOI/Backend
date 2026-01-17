package com.example.scoi.domain.charge.controller;


import com.example.scoi.domain.charge.dto.ChargeResDTO;
import com.example.scoi.domain.charge.exception.ChargeException;
import com.example.sco.domain.charge.exception.code.ChargeErrorCode;
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
    @Operation(summary = "보유 자산 조회", description = "현재 보유 자산을 조회합니다. 충전 메인 화면을 위한 API로 스냅샷 형태입니다! 실시간 아닙니다!!")
    @SecurityRequirement(name = "JWT TOKEN")
    public ApiResponse<ChargeResDTO.BalanceDTO> getBalances(
            @RequestParam(defaultValue = "Bithumb") String exchangeType,
            /* 임시 파라미터: JWT 인증 필터/인터셉터 구현 전까지 사용
                            이후 이 파라미터를 제거 JWT 토큰에서 memberId를 추출하여 사용
              @RequestParam 대신 다른  어노테이션 사용
             
             현재는 로컬 테스트를 위해 Query Parameter로 받고 있음
             */
            @RequestParam Long memberId
            
            // 임시 테스트용: Member 없이 테스트할 때 주석 해제
            // @RequestParam String phoneNumber
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
        
        //  임시 테스트용: phoneNumber 직접 사용 (위 코드 주석 처리하고 이 코드 주석 해제)
        // ChargeResDTO.BalanceDTO result = chargeService.getBalances(phoneNumber, exchangeTypeEnum);
        
        return ApiResponse.onSuccess(ChargeSuccessCode.BALANCE_INQUIRY_SUCCESS, result);
    }

}
