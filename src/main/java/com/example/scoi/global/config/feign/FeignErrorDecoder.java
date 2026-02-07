//에러 처리
package com.example.scoi.global.config.feign;

import com.example.scoi.global.client.dto.ClientErrorDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        String responseBody = "";
        
        try {
            // 응답 본문 읽기
            if (response.body() != null) {
                InputStream bodyStream = response.body().asInputStream();
                responseBody = new String(bodyStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warn("응답 본문 읽기 실패: {}", e.getMessage());
        }
        
        // JSON 파싱 시도
        if (!responseBody.isEmpty()) {
            String trimmedBody = responseBody.trim();
            boolean isJsonFormat = trimmedBody.startsWith("{") && trimmedBody.endsWith("}");
            
            if (isJsonFormat) {
                try {
                    ClientErrorDTO.Errors errorResponse = objectMapper.readValue(responseBody, ClientErrorDTO.Errors.class);
                    if (errorResponse != null && errorResponse.error() != null) {
                        // JSON 에러 응답 파싱 성공
                        log.error("Feign Client 에러 발생 - methodKey: {}, status: {}, reason: {}, errorName: {}, errorMessage: {}", 
                                methodKey, response.status(), response.reason(), 
                                errorResponse.error().name(), errorResponse.error().message());
                    } else {
                        log.error("Feign Client 에러 발생 - methodKey: {}, status: {}, reason: {}, responseBody: {}", 
                                methodKey, response.status(), response.reason(), responseBody);
                    }
                } catch (Exception e) {
                    // JSON 파싱 실패 - 원본 응답 본문 로깅
                    log.error("Feign Client 에러 발생 (JSON 파싱 실패) - methodKey: {}, status: {}, reason: {}, 파싱 에러: {}, responseBody: {}", 
                            methodKey, response.status(), response.reason(), e.getMessage(), responseBody);
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
        
        return FeignException.errorStatus(methodKey, response);
    }
}
