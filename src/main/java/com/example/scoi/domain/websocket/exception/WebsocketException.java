package com.example.scoi.domain.websocket.exception;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import com.example.scoi.global.apiPayload.exception.ScoiException;

public class WebsocketException extends ScoiException {
    public WebsocketException(BaseErrorCode code) {
        super(code);
    }
}
