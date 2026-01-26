package com.example.scoi.domain.member.exception.code;

import com.example.scoi.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberSuccessCode implements BaseSuccessCode {

    OK(HttpStatus.OK,
            "MEMBER200_1",
            "회원 정보를 조회했습니다."),
    CHANGE_PHONE_NUMBER(HttpStatus.OK,
            "MEMBER200_2",
            "휴대폰 번호가 변경되었습니다."),
    CHANGE_SIMPLE_PASSWORD(HttpStatus.OK,
            "MEMBER200_3",
            "비밀번호가 변경되었습니다."),
    RESET_SIMPLE_PASSWORD(HttpStatus.OK,
            "MEMBER200_4",
            "비밀번호가 재설정되었습니다."),
    GET_API_KEY_LIST(HttpStatus.OK,
            "MEMBER200_5",
            "API키 목록을 조회했습니다."),
    POST_PATCH_API_KEY(HttpStatus.CREATED,
            "MEMBER200_6",
            "API키가 등록, 수정되었습니다."),
    DELETE_API_KEY(HttpStatus.OK,
            "MEMBER200_7",
            "API 키가 삭제되었습니다."),
    POST_PATCH_FCM_TOKEN(HttpStatus.OK,
            "MEMBER200_8",
            "FCM 토큰이 추가되었습니다."),

    EXCHANGE_LIST(HttpStatus.OK,
            "EXCHANGE200_1",
            "거래소 목록 조회에 성공했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
