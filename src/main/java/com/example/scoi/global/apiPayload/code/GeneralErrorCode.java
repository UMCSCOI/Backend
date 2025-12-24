package com.example.scoi.global.apiPayload.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GeneralErrorCode implements BaseErrorCode{

    BAD_REQUEST(HttpStatus.BAD_REQUEST,
            "COMMON400_0",
            "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED,
            "COMMON401_0",
            "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN,
            "COMMON403_0",
            "권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND,
            "COMMON404_0",
            "요청에 맞는 데이터를 찾지 못했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,
            "COMMON500_0",
            "서버에서 처리하지 못했습니다."),

    // 검증용 에러 메시지
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST,
            "VALID400_0",
            "검증에 실패했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
