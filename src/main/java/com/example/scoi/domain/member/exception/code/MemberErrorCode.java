package com.example.scoi.domain.member.exception.code;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {

    INVALIDED_PASSWORD(HttpStatus.BAD_REQUEST,
            "MEMBER400_1",
            "간편 비밀번호가 틀렸습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND,
            "MEMBER404_1",
            "사용자를 찾지 못했습니다."),
    API_KEY_NOT_FOUND(HttpStatus.NOT_FOUND,
            "MEMBER404_2",
            "API 키를 찾을 수 없습니다."),
    FCM_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND,
            "MEMBER404_3",
            "FCM토큰을 찾을 수 없습니다."),
    ALREADY_REGISTER_API_KEY(HttpStatus.CONFLICT,
            "MEMBER409_2",
            "이미 등록된 거래소입니다."),
    // 임시
    LOCKED(HttpStatus.FORBIDDEN,
            "AUTH403_1",
            "5회 이상 실패하여 비밀번호가 잠겼습니다. 재설정이 필요합니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
