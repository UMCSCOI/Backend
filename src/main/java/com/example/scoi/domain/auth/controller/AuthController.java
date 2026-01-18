package com.example.scoi.domain.auth.controller;

import com.example.scoi.domain.auth.dto.AuthReqDTO;
import com.example.scoi.domain.auth.dto.AuthResDTO;
import com.example.scoi.domain.auth.service.AuthService;
import com.example.scoi.global.apiPayload.ApiResponse;
import com.example.scoi.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증 API", description = "SMS, 회원가입, 로그인, 토큰 관리")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "SMS 발송", description = "휴대폰 번호로 인증번호를 발송합니다.")
    @PostMapping("/sms/send")
    public ResponseEntity<ApiResponse<Void>> sendSms(
            @Valid @RequestBody AuthReqDTO.SmsSendRequest request
    ) {
        authService.sendSms(request);
        return ResponseEntity.ok(ApiResponse.onSuccess(GeneralSuccessCode.OK));
    }

    @Operation(summary = "SMS 검증", description = "인증번호를 검증합니다. result: null, isExistingMember 삭제")
    @PostMapping("/sms/verify")
    public ResponseEntity<ApiResponse<AuthResDTO.SmsVerifyResponse>> verifySms(
            @Valid @RequestBody AuthReqDTO.SmsVerifyRequest request
    ) {
        AuthResDTO.SmsVerifyResponse response = authService.verifySms(request);
        return ResponseEntity.ok(ApiResponse.onSuccess(GeneralSuccessCode.OK, response));
    }

    @Operation(summary = "회원가입", description = "회원가입을 진행합니다. 409로 기존회원 판단, result: null")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(
            @Valid @RequestBody AuthReqDTO.SignupRequest request
    ) {
        authService.signup(request);
        return ResponseEntity.ok(ApiResponse.onSuccess(GeneralSuccessCode.OK));
    }

    @Operation(summary = "로그인", description = "로그인 후 Access Token과 Refresh Token을 반환합니다. isBioRegistered 삭제")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResDTO.LoginResponse>> login(
            @Valid @RequestBody AuthReqDTO.LoginRequest request
    ) {
        AuthResDTO.LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.onSuccess(GeneralSuccessCode.OK, response));
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 Access Token과 Refresh Token을 모두 재발급합니다.")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<AuthResDTO.ReissueResponse>> reissue(
            @Valid @RequestBody AuthReqDTO.ReissueRequest request
    ) {
        AuthResDTO.ReissueResponse response = authService.reissue(request);
        return ResponseEntity.ok(ApiResponse.onSuccess(GeneralSuccessCode.OK, response));
    }

    @Operation(summary = "로그아웃", description = "로그아웃 처리 (Refresh Token 삭제, Access Token 블랙리스트 등록)")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authorization
    ) {
        // TODO: 다음 PR에서 JWT 구현 후 phoneNumber 추출
        String phoneNumber = "01012345678"; // 임시
        String accessToken = authorization.replace("Bearer ", "");

        authService.logout(phoneNumber, accessToken);
        return ResponseEntity.ok(ApiResponse.onSuccess(GeneralSuccessCode.OK));
    }
}