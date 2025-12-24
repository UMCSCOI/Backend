package com.example.scoi.global.apiPayload;

import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import com.example.scoi.global.apiPayload.code.BaseSuccessCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@NotNull
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class ApiResponse<T> {

    @JsonProperty("isSuccess")
    private final Boolean isSuccess;

    @JsonProperty("code")
    private final String code;

    @JsonProperty("message")
    private final String message;

    @JsonProperty("result")
    private final T result;

    // 성공: 결과 O
    public static <T> ApiResponse<T> onSuccess(BaseSuccessCode code, T result) {
        return new ApiResponse<>(true, code.getCode(), code.getMessage(), result);
    }

    // 성공: 결과 X
    public static <T> ApiResponse<T> onSuccess(BaseSuccessCode code) {
        return new ApiResponse<>(true, code.getCode(), code.getMessage(), null);
    }

    // 실패: 결과 O
    public static <T> ApiResponse<T> onFailure(BaseErrorCode code, T result) {
        return new ApiResponse<>(false, code.getCode(), code.getMessage(), result);
    }

    // 실패: 결과 X
    public static <T> ApiResponse<T> onFailure(BaseErrorCode code) {
        return new ApiResponse<>(false, code.getCode(), code.getMessage(), null);
    }
}
