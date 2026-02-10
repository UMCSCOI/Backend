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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

public class TransferReqDTO {

    // 출금 시 필요한 수취인 값
    public record RecipientInformation(
            @Schema(description = "수취인 유형 (개인/법인)", example = "INDIVIDUAL", allowableValues = {"INDIVIDUAL", "CORPORATION"})
            @NotNull(message = "수취인 유형은 필수입니다.")
            MemberType memberType,

            @Schema(description = "수취인 국문 이름", example = "김철수")
            @NotBlank(message = "수취인 이름은 필수입니다.")
            @Pattern(regexp = "^[가-힣]+$", message = "한글만 입력 가능합니다.")
            @Size(min = 2, max = 5, message = "한글 이름은 2~5자입니다.")
            String recipientKoName,

            @Schema(description = "수취인 영문 이름(빗썸 이체 시 필요)", example = "김철수")
            @Pattern(regexp = "^[A-Z ]*$", message = "영문 대문자와 공백만 입력 가능합니다.")
            @Size(max = 50, message = "영문 이름은 50자 이내입니다.")
            String recipientEnName,

//            String recipientCorpKoName,
//            String recipientCorpEnName,

            @Schema(description = "수취인 지갑 주소", example = "TQ6r2x3B9kzJHf8M4YpP1CwD7eRVaS5tKU")
            @NotBlank(message = "수취인 지갑 주소는 필수입니다.")
            String walletAddress,

            @Schema(description = "상대방 지갑 주소 거래소 (빗썸 전용)", example = "UPBIT", allowableValues = {"UPBIT", "BITHUMB"})
            ExchangeType exchangeType,

            @Schema(description = "화폐 코드 (대문자)", example = "USDT")
            @NotNull(message = "화폐 코드는 필수입니다.")
            CoinType coinType,

            @Schema(description = "네트워크 타입 (출금 체인)", example = "TRX")
            @NotNull(message = "네트워크 타입은 필수입니다.")
            NetworkType netType
    ) { }

    // 견적 검증 시 필요한 값
    public record Quote(
            @Schema(description = "출금 가능 금액", example = "10")
            @NotBlank(message = "출금 가능 금액은 필수입니다.")
            @Pattern(regexp = "^[0-9]+$", message = "정수만 입력 가능합니다.")
            String available,

            @Schema(description = "출금할 금액", example = "5")
            @NotBlank(message = "출금할 금액은 필수입니다.")
            @Pattern(regexp = "^[0-9]+$", message = "정수만 입력 가능합니다.")
            String amount,

            @Schema(description = "화폐 코드 (대문자)", example = "USDT")
            CoinType coinType,

            @Schema(description = "네트워크 타입 (출금 체인)", example = "TRX")
            NetworkType networkType,

            @Schema(description = "네트워크 수수료", example = "1")
            @NotBlank(message = "네트워크 수수료는 필수입니다.")
            @Pattern(regexp = "^[0-9]+$", message = "정수만 입력 가능합니다.")
            String networkFee
    ) {}


    public record WithdrawRequest(
            @Schema(description = "화폐 코드 (대문자)", example = "USDT")
            @NotBlank(message = "화폐 코드는 필수입니다.")
            String currency,

            @Schema(description = "네트워크 타입 (출금 체인)", example = "TRX")
            @NotNull(message = "네트워크 타입은 필수입니다.")
            NetworkType netType,

            @Schema(description = "출금 수량 (소수점X)", example = "3")
            @NotBlank(message = "출금 수량은 필수입니다.")
            @Pattern(regexp = "^[0-9]+$", message = "정수만 입력 가능합니다.")
            String amount,

            @Schema(description = "출금 대상 지갑 주소", example = "TQ6r2x3B9kzJHf8M4YpP1CwD7eRVaS5tKU")
            @NotBlank(message = "출금 주소는 필수입니다.")
            String address,

            @Schema(description = "상대방 지갑 주소 거래소 (빗썸 전용)", example = "UPBIT", allowableValues = {"UPBIT", "BITHUMB"})
            ExchangeType exchangeName,

            @Schema(description = "출금 실행 거래소 타입(내 API키가 등록된 거래소)", example = "UPBIT", allowableValues = {"UPBIT", "BITHUMB"})
            @NotNull(message = "거래소 타입은 필수입니다.")
            ExchangeType exchangeType,

            @Schema(description = "수취인 유형 (빗썸 전용, 상대방 거래소)", example = "INDIVIDUAL", allowableValues = {"INDIVIDUAL", "CORPORATION"})
            MemberType receiverType,

            @Schema(description = "수취인 성명 (국문, 빗썸 전용)", example = "김철수")
            String receiverKoName,

            @Schema(description = "수취인 성명 (영문, 빗썸 전용)", example = "KIMCHULSU")
            String receiverEnName,

//            @Schema(description = "법인 국문명 (법인일 경우만)", example = "(주)스카이")
//            String receiverCorpKoName,
//
//            @Schema(description = "법인 영문명 (법인일 경우만)", example = "SCOI Co., Ltd.")
//            String receiverCorpEnName,

            @Schema(description = "간편 비밀번호", example = "6v4RsQ+gOGi1NtheSTiA1w==")
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
            NetworkType netType,
            String amount,
            String address
    ) {}

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 JSON 생성 시 제외됨
    public record BithumbWithdrawRequest(
            @JsonProperty("currency")
            String currency,
            @JsonProperty("net_type")
            NetworkType netType,
            @JsonProperty("amount")
            Double amount, // 명세서의 Number 타입을 위해 Double로 변경
            @JsonProperty("address")
            String address,
            @JsonProperty("exchange_name")
            String exchangeName, // 상대방 출금 거래소
            @JsonProperty("receiver_type")
            MemberType receiverType, // personal 또는 corporation

            // 수취인(대표자) 정보
            @JsonProperty("receiver_ko_name")
            String receiverKoName,
            @JsonProperty("receiver_en_name")
            String receiverEnName

//            // 법인 정보 (receiverType이 corporation일 때만 포함됨)
//            @JsonProperty("receiver_corp_ko_name")
//            String receiverCorpKoName,
//            @JsonProperty("receiver_corp_en_name")
//            String receiverCorpEnName
    ) {}
}
