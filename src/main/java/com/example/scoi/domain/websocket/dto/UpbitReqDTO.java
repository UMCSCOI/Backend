package com.example.scoi.domain.websocket.dto;

import lombok.Builder;

import java.util.List;

public class UpbitReqDTO {

    // 가격 실시간 조회
    @Builder
    public record Ticker(
            String type,
            List<String> codes
    ){}
}
