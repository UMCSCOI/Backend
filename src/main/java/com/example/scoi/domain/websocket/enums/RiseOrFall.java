package com.example.scoi.domain.websocket.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RiseOrFall {
    RISE("올랐어요."),
    FALL("떨어졌어요.")
    ;

    private final String message;
}
