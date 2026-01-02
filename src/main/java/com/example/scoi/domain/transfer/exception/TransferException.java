package com.example.scoi.domain.transfer.exception;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import com.example.scoi.global.apiPayload.exception.ScoiException;

public class TransferException extends ScoiException {

    public TransferException(BaseErrorCode code) {
        super(code);
    }
}
