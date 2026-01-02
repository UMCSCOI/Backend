package com.example.scoi.domain.invest.exception;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import com.example.scoi.global.apiPayload.exception.ScoiException;

public class InvestException extends ScoiException {

    public InvestException(BaseErrorCode code) {
        super(code);
    }
}
