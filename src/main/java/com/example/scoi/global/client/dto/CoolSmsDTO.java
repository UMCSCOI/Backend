package com.example.scoi.global.client.dto;

public class CoolSmsDTO {

    public record SendRequest(
        String to,
        String from,
        String text
    ) {}

    public record SendResponse(
        String groupId,
        String messageId,
        String statusCode,
        String statusMessage
    ) {}
}
