package com.example.scoi.domain.myWallet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 거래소 주문 리스트 API 원본 응답 DTO
 * global/client/dto 수정 없이 myWallet 도메인 내에 정의
 */
public class TopupClientDTO {

    /**
     * 빗썸 주문 리스트 조회 응답
     * GET /v1/orders
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BithumbOrder(
            String uuid,
            String side,
            @JsonProperty("ord_type") String ordType,
            String price,
            String state,
            String market,
            String volume,
            @JsonProperty("remaining_volume") String remainingVolume,
            @JsonProperty("executed_volume") String executedVolume,
            @JsonProperty("created_at") String createdAt
    ) {}

    /**
     * 업비트 주문 목록 조회 응답
     * GET /v1/orders/closed, GET /v1/orders/open
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UpbitOrder(
            String uuid,
            String side,
            @JsonProperty("ord_type") String ordType,
            String price,
            String state,
            String market,
            String volume,
            @JsonProperty("remaining_volume") String remainingVolume,
            @JsonProperty("executed_volume") String executedVolume,
            @JsonProperty("created_at") String createdAt
    ) {}
}
