package com.example.scoi.domain.transfer.exception.code;

import com.example.scoi.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TransferSuccessCode implements BaseSuccessCode {

    TRANSFER200_1(HttpStatus.FOUND, "TRANSFER200_1", "수취인 목록 조회에 성공하였습니다.")
    TRANSFER200_1(HttpStatus.FOUND, "TRANSFER200_1", "최근 수취인 목록 조회에 성공하였습니다."),
    TRANSFER200_2(HttpStatus.FOUND, "TRANSFER200_2", "즐겨찾기 수취인 조회에 성공했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
