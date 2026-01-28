package com.example.scoi.global.client.dto;

public class CoolSmsDTO {

    public record SendRequest(
        Message message
    ) {
        public SendRequest(String to, String from, String text) {
            this(new Message(to, from, text));
        }
    }

    public record Message(
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
