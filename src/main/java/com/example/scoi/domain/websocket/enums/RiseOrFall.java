package com.example.scoi.domain.websocket.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum RiseOrFall {
    RISE("올랐어요."),
    FALL("떨어졌어요.")
    ;

    private final String message;
}
