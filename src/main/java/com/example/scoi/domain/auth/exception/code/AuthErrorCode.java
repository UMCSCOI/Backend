package com.example.scoi.domain.auth.exception.code;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    // SMS 관련
    SMS_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,
            "AUTH500_1",
            "SMS 발송에 실패했습니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST,
            "AUTH400_1",
            "인증번호가 일치하지 않습니다."),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST,
            "AUTH400_2",
            "인증번호가 만료되었습니다."),
    VERIFICATION_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST,
            "AUTH400_3",
            "SMS 인증 토큰이 만료되었거나 유효하지 않습니다."),

    // 회원가입/로그인 관련
    ALREADY_REGISTERED_PHONE(HttpStatus.CONFLICT,
            "AUTH409_1",
            "이미 가입된 휴대폰 번호입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND,
            "AUTH404_1",
            "존재하지 않는 회원입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED,
            "AUTH401_1",
            "비밀번호가 일치하지 않습니다."),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN,
            "AUTH403_1",
            "5회 이상 비밀번호 오류로 계정이 잠겼습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN,
            "AUTH403_2",
            "접근 권한이 없습니다."),

    // 토큰 관련
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED,
            "AUTH401_0",
            "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED,
            "AUTH401_2",
            "유효하지 않은 토큰입니다."),
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED,
            "AUTH401_3",
            "만료된 Access Token입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED,
            "AUTH401_4",
            "Refresh Token이 만료되었습니다. 다시 로그인해주세요."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED,
            "AUTH401_5",
            "Refresh Token을 찾을 수 없습니다."),
    BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED,
            "AUTH401_6",
            "블랙리스트에 등록된 토큰입니다. 다시 로그인해주세요."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}