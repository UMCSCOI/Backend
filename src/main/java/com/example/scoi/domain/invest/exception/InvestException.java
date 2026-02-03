package com.example.scoi.domain.invest.exception;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import com.example.scoi.global.apiPayload.exception.ScoiException;

import java.util.Map;

public class InvestException extends ScoiException {

    public InvestException(BaseErrorCode code) {
        super(code);
    }

    public InvestException(BaseErrorCode code, Map<String, String> bind) {
        super(code, bind);
    }
}
