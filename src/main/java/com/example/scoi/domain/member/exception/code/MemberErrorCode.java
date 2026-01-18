package com.example.scoi.domain.member.exception.code;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {

    // 도메인별 실패 코드 정의
    NOT_FOUND(HttpStatus.NOT_FOUND,
            "MEMBER404_0",
            "사용자를 찾지 못했습니다."),
    
    // 간편 비밀번호 검증 실패
    INVALID_SIMPLE_PASSWORD(HttpStatus.BAD_REQUEST,
            "AUTH400_2",
            "인증번호가 일치하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
