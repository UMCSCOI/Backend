package com.example.scoi.domain.member.controller;

import com.example.scoi.domain.member.dto.MemberReqDTO;
import com.example.scoi.domain.member.dto.MemberResDTO;
import com.example.scoi.global.apiPayload.ApiResponse;
import com.example.scoi.global.auth.entity.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;

@Tag(name = "마이페이지 API")
public interface MemberControllerDocs {

    @Operation(
            summary = "내 정보 조회 API By 김주헌",
            description = "로그인한 사용자의 정보 조회합니다."
    )
    ApiResponse<MemberResDTO.MemberInfo> getMemberInfo(@AuthenticationPrincipal AuthUser user);

    @Operation(
            summary = "휴대폰 번호 변경 API By 김주헌 (개발 중)",
            description = "SMS 인증 완료 후 휴대폰 번호 변경합니다."
    )
    ApiResponse<MemberResDTO.ChangePhone> changePhone(@RequestBody MemberReqDTO.ChangePhone dto, @AuthenticationPrincipal AuthUser user);

    @Operation(
            summary = "간편 비밀번호 변경 API By 김주헌",
            description = "간편 비밀번호를 변경합니다."
    )
    ApiResponse<Map<String, String>> changePassword(@RequestBody MemberReqDTO.ChangePassword dto, @AuthenticationPrincipal AuthUser user) throws GeneralSecurityException;

    @Operation(
            summary = "간편 비밀번호 재설정 API By 김주헌 (개발 중)",
            description = "비밀번호 분실 또는 5회 실패 시 재설정을 합니다."
    )
    ApiResponse<Void> resetPassword(@RequestBody MemberReqDTO.ResetPassword dto, @AuthenticationPrincipal AuthUser user);

    @Operation(
            summary = "거래소 목록 조회 API By 김주헌",
            description = "지원 거래소 목록과, 사용자 기준으로 지원 거래소와 연동 상태를 조회합니다."
    )
    ApiResponse<List<MemberResDTO.ExchangeList>> getExchangeList(@AuthenticationPrincipal AuthUser user);

    @Operation(
            summary = "API키 목록 조회 API By 김주헌",
            description = "연동된 거래소의 API키 목록 조회합니다."
    )
    ApiResponse<List<MemberResDTO.ApiKeyList>> getApiKeyList(@AuthenticationPrincipal AuthUser user);

    @Operation(
            summary = "API키 등록 및 수정 API By 김주헌",
            description = "연동된 거래소의 API키를 등록 및 수정을 합니다."
    )
    ApiResponse<List<String>> postPatchApiKey(@AuthenticationPrincipal AuthUser user, @RequestBody List<MemberReqDTO.PostPatchApiKey> dto);

    @Operation(
            summary = "API키 삭제 API By 김주헌",
            description = "연동된 거래소의 API키를 삭제합니다."
    )
    ApiResponse<Void> deleteApiKey(@AuthenticationPrincipal AuthUser user, @RequestBody MemberReqDTO.DeleteApiKey dto);

    @Operation(
            summary = "FCM 토큰 등록 API By 김주헌",
            description = "FCM 토큰을 등록합니다."
    )
    ApiResponse<Void> postFcmToken(@AuthenticationPrincipal AuthUser user, @RequestBody MemberReqDTO.PostFcmToken dto);
}
