package com.example.scoi.domain.transfer.exception.code;

import com.example.scoi.global.apiPayload.code.BaseSuccessCode;
import com.google.api.Http;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TransferSuccessCode implements BaseSuccessCode {

    TRANSFER200_1(HttpStatus.FOUND, "TRANSFER200_1", "최근 수취인 목록 조회에 성공하였습니다."),
    TRANSFER200_2(HttpStatus.FOUND, "TRANSFER200_2", "즐겨찾기 수취인 조회에 성공했습니다."),
    TRANSFER200_3(HttpStatus.OK, "TRANSFER200_3", "즐겨찾기 수취인으로 변경했습니다."),
    TRANSFER200_4(HttpStatus.OK, "TRANSFER200_4", "즐겨찾기 수취인에서 해제했습니다."),
    TRANSFER200_5(HttpStatus.OK, "TRANSFER200_5", "수취인 입력값 검증에 성공했습니다."),

    TRANSFER201_1(HttpStatus.CREATED, "TRANSFER201_1", "즐겨찾기 수취인 등록에 성공했습니다.")
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
