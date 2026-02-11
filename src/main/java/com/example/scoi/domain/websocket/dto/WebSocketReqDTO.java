package com.example.scoi.domain.websocket.dto;

import lombok.Builder;

public class WebSocketReqDTO {

    @Builder
    public record Ticket(
            String ticket
    ){}

    @Builder
    public record Format(
            String format
    ){}
}
