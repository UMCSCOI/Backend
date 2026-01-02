package com.example.scoi.domain.charge.exception;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import com.example.scoi.global.apiPayload.exception.ScoiException;

public class ChargeException extends ScoiException {
    public ChargeException(BaseErrorCode code) {
        super(code);
    }
}
