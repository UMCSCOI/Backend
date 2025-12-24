package com.example.scoi.domain.test;

import com.example.scoi.global.apiPayload.ApiResponse;
import com.example.scoi.global.apiPayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {

    @GetMapping("/test")
    public ApiResponse<String > test(){
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, "test");
    }

    @GetMapping("/exception")
    public ApiResponse<String> exception(){
        throw new TestException(TestErrorCode.TEST);
    }
}
