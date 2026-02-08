//에러 처리
package com.example.scoi.global.config.feign;

import com.example.scoi.global.client.dto.ClientErrorDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        String responseBody = "";

        // 응답 본문 읽기
        try {
            if (response.body() != null) {
                byte[] bodyBytes = response.body().asInputStream().readAllBytes();
                responseBody = new String(bodyBytes, StandardCharsets.UTF_8);

                // body를 다시 설정하여 FeignException에서 접근 가능하도록 함
                response = response.toBuilder().body(bodyBytes).build();

                // JSON 파싱 시도
                if (!responseBody.isEmpty()) {
                    // JSON 형식인지 확인 (중괄호로 시작하고 닫히는지)
                    String trimmedBody = responseBody.trim();
                    boolean isJsonFormat = trimmedBody.startsWith("{") && trimmedBody.endsWith("}");

                    if (isJsonFormat) {
                        try {
                            ClientErrorDTO.Errors errorResponse = objectMapper.readValue(responseBody, ClientErrorDTO.Errors.class);
                            if (errorResponse != null && errorResponse.error() != null) {
                                // JSON 에러 응답 파싱 성공 - error.name과 error.message 로깅
                                log.error("Feign Client 에러 발생 - methodKey: {}, status: {}, reason: {}, errorName: {}, errorMessage: {}",
                                        methodKey, response.status(), response.reason(),
                                        errorResponse.error().name(), errorResponse.error().message());
                            } else {
                                log.error("Feign Client 에러 발생 - methodKey: {}, status: {}, reason: {}, responseBody: {}",
                                        methodKey, response.status(), response.reason(), responseBody);
                            }
                        } catch (Exception e) {
                            // JSON 파싱 실패 - 원본 응답 본문 로깅
                            log.error("Feign Client 에러 발생 (JSON 파싱 실패) - methodKey: {}, status: {}, reason: {}, 파싱 에러: {}",
                                    methodKey, response.status(), response.reason(), e.getMessage());
                            log.error("응답 본문 전체 내용: {}", responseBody);
                        }
                    } else {
                        // JSON 형식이 아닌 경우
                        log.error("Feign Client 에러 발생 (비JSON 응답) - methodKey: {}, status: {}, reason: {}, responseBody: {}",
                                methodKey, response.status(), response.reason(), responseBody);
                    }
                } else {
                    log.error("Feign Client 에러 발생 - methodKey: {}, status: {}, reason: {}",
                            methodKey, response.status(), response.reason());
                }
            } else {
                log.error("Feign Client 에러 발생 - methodKey: {}, status: {}, reason: {}",
                        methodKey, response.status(), response.reason());
            }
        } catch (IOException e) {
            log.error("Feign Client 에러 발생 - methodKey: {}, status: {}, reason: {} (응답 본문 읽기 실패: {})",
                    methodKey, response.status(), response.reason(), e.getMessage());
        }

        // FeignException을 그대로 반환하여 상위에서 세부적인 분기 가능
        return FeignException.errorStatus(methodKey, response);
    }
}
