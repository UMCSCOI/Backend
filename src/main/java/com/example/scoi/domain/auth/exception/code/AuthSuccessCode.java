package com.example.scoi.domain.auth.code;

import com.example.scoi.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthSuccessCode implements BaseSuccessCode {

    SMS_SENT(HttpStatus.OK,
            "AUTH200_1",
            "SMS를 성공적으로 발송했습니다."),
    SMS_VERIFIED(HttpStatus.OK,
            "AUTH200_2",
            "SMS 인증에 성공했습니다."),
    SIGNUP_SUCCESS(HttpStatus.CREATED,
            "AUTH201_1",
            "회원가입이 완료되었습니다."),
    LOGIN_SUCCESS(HttpStatus.OK,
            "AUTH200_3",
            "로그인에 성공하였습니다."),
    TOKEN_REISSUED(HttpStatus.OK,
            "AUTH200_4",
            "토큰이 재발급되었습니다."),
    LOGOUT_SUCCESS(HttpStatus.OK,
            "AUTH200_5",
            "로그아웃되었습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}