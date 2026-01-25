package com.example.scoi.domain.member.exception;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import com.example.scoi.global.apiPayload.exception.ScoiException;

import java.util.Map;

public class MemberException extends ScoiException {
    public MemberException(BaseErrorCode code) {
        super(code);
    }
    public MemberException(BaseErrorCode code, Map<String, String> binding) {super(code,binding);}
}
