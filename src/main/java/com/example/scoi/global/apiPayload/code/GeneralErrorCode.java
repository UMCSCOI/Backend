package com.example.scoi.global.apiPayload.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GeneralErrorCode implements BaseErrorCode{

    BAD_REQUEST(HttpStatus.BAD_REQUEST,
            "COMMON400_1",
            "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED,
            "COMMON401_1",
            "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN,
            "COMMON403_1",
            "권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND,
            "COMMON404_1",
            "요청에 맞는 데이터를 찾지 못했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,
            "COMMON500_1",
            "서버에서 처리하지 못했습니다."),

    // 검증용 에러 메시지
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST,
            "VALID400_1",
            "검증에 실패했습니다."),
    NOT_SUPPORT_HTTP_METHOD(HttpStatus.BAD_REQUEST,
            "VALID400_2",
            "잘못된 HTTP 메서드입니다."),
    JSON_PARSE_FAIL(HttpStatus.BAD_REQUEST,
            "VALID400_3",
            "JSON 파싱에 실패했습니다. Request Body를 확인해주세요."),
    PARAMETER_MISMATCH(HttpStatus.BAD_REQUEST,
            "VALID400_4",
            "쿼리 파라미터 타입이 맞지 않습니다."),
    NOT_FOUND_REQUEST_BODY(HttpStatus.NOT_FOUND,
            "VALID404_1",
            "Request Body가 없습니다."),
    NOT_FOUND_URI(HttpStatus.NOT_FOUND,
            "VALID404_2",
            "존재하지 않는 URI입니다."),
    NOT_SUPPORT_CONTENT_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "VALID415_1",
            "지원하지 않는 Content-Type입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
