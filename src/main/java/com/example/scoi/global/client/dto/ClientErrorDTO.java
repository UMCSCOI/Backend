package com.example.scoi.global.client.dto;

import lombok.Builder;

public class ClientErrorDTO {

    @Builder
    public record Errors(
            Error error
    ){}

    @Builder
    public record Error(
            String message,
            String name
    ){}
}
