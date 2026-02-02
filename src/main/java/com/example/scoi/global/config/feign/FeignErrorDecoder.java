//에러 처리
package com.example.scoi.global.config.feign;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("Feign Client 에러 발생 - methodKey: {}, status: {}, reason: {}", 
                methodKey, response.status(), response.reason());
        return new RuntimeException("Feign Client 호출 실패: " + response.status() + " " + response.reason());
    }
}
