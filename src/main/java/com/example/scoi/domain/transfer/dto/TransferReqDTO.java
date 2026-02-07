package com.example.scoi.domain.transfer.dto;

import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.member.enums.MemberType;
import com.example.scoi.domain.transfer.enums.CoinType;
import com.example.scoi.domain.transfer.enums.NetworkType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

public class TransferReqDTO {

    public record RecipientInformation(
            MemberType memberType,
            String recipientName,
            String corpKoreanName,
            String corpEnglishName,
            String walletAddress,
            ExchangeType exchangeType,
            CoinType coinType,
            String netType
    ) { }

    public record Quote(
            String available,
            String amount,
            CoinType coinType,
            NetworkType networkType,
            String networkFee
    ) {}


    public record WithdrawRequest(
            @Schema(description = "화폐 코드 (대문자)", example = "USDT")
            @NotBlank(message = "화폐 코드는 필수입니다.")
            String currency,

            @Schema(description = "네트워크 타입 (출금 체인)", example = "TRX")
            @NotBlank(message = "네트워크 타입은 필수입니다.")
            String netType,

            @Schema(description = "출금 수량 (소수점X)", example = "10")
            @NotBlank(message = "출금 수량은 필수입니다.")
            String amount,

            @Schema(description = "출금 대상 지갑 주소", example = "TQ6r2x3B9kzJHf8M4YpP1CwD7eRVaS5tKU")
            @NotBlank(message = "출금 주소는 필수입니다.")
            String address,

            @Schema(description = "상대방 출금 거래소", example = "UPBIT", allowableValues = {"UPBIT", "BITHUMB"})
            @NotNull(message = "거래소 타입은 필수입니다.")
            ExchangeType exchangeName,

            @Schema(description = "출금 실행 거래소 타입", example = "UPBIT", allowableValues = {"UPBIT", "BITHUMB"})
            @NotNull(message = "거래소 타입은 필수입니다.")
            ExchangeType exchangeType,

            @Schema(description = "수취인 유형 (빗썸 전용)", example = "INDIVIDUAL", allowableValues = {"INDIVIDUAL", "CORPORATION"})
            String receiverType,

            @Schema(description = "수취인 성명 (국문)", example = "김철수")
            String receiverKoName,

            @Schema(description = "수취인 성명 (영문)", example = "KIMCHULSU")
            String receiverEnName,

            @Schema(description = "법인 국문명 (법인일 경우만)", example = "(주)스카이")
            String receiverCorpKoName,

            @Schema(description = "법인 영문명 (법인일 경우만)", example = "SCOI Co., Ltd.")
            String receiverCorpEnName,

            @Schema(description = "간편 비밀번호 (6자리)", example = "123456")
            @NotBlank(message = "비밀번호는 필수입니다.")
            String simplePassword,

            @Schema(description = "멱등성 키 (중복 요청 방지용 UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
            @NotBlank(message = "멱등성 키는 필수입니다.")
            String idempotentKey
    ) {}

    @Builder
    public record UpbitWithdrawRequest(
            String currency,
            @JsonProperty("net_type")
            String netType,
            String amount,
            String address
    ) {}

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 JSON 생성 시 제외됨
    public record BithumbWithdrawRequest(
            @JsonProperty("currency")
            String currency,
            @JsonProperty("net_type")
            String netType,
            @JsonProperty("amount")
            Double amount, // 명세서의 Number 타입을 위해 Double로 변경
            @JsonProperty("address")
            String address,
            @JsonProperty("exchange_name")
            String exchangeName, // 상대방 출금 거래소
            @JsonProperty("receiver_type")
            String receiverType, // personal 또는 corporation

            // 수취인(대표자) 정보
            @JsonProperty("receiver_ko_name")
            String receiverKoName,
            @JsonProperty("receiver_en_name")
            String receiverEnName,

            // 법인 정보 (receiverType이 corporation일 때만 포함됨)
            @JsonProperty("receiver_corp_ko_name")
            String receiverCorpKoName,
            @JsonProperty("receiver_corp_en_name")
            String receiverCorpEnName
    ) {}
}
