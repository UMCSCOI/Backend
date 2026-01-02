package com.example.scoi.domain.myWallet.exception;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import com.example.scoi.global.apiPayload.exception.ScoiException;

public class MyWalletException extends ScoiException {

    public MyWalletException(BaseErrorCode code) {
        super(code);
    }
}
