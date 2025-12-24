package com.example.scoi.domain.test;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import com.example.scoi.global.apiPayload.exception.ScoiException;

public class TestException extends ScoiException {
    public TestException(BaseErrorCode code) {
        super(code);
    }
}
