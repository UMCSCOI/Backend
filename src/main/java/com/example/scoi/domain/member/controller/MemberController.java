package com.example.scoi.domain.member.controller;

import com.example.scoi.domain.member.dto.MemberReqDTO;
import com.example.scoi.domain.member.dto.MemberResDTO;
import com.example.scoi.domain.member.entity.Member;
import com.example.scoi.domain.member.exception.MemberException;
import com.example.scoi.domain.member.exception.code.MemberErrorCode;
import com.example.scoi.domain.member.exception.code.MemberSuccessCode;
import com.example.scoi.domain.member.repository.MemberRepository;
import com.example.scoi.domain.member.service.MemberService;
import com.example.scoi.global.apiPayload.ApiResponse;
import com.example.scoi.global.apiPayload.code.BaseErrorCode;
import com.example.scoi.global.apiPayload.code.BaseSuccessCode;
import com.example.scoi.global.apiPayload.code.GeneralErrorCode;
import com.example.scoi.global.apiPayload.code.GeneralSuccessCode;
import com.example.scoi.global.auth.entity.AuthUser;
import com.example.scoi.global.auth.userDetails.CustomUserDetails;
import com.example.scoi.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MemberController implements MemberControllerDocs{

    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;

    // 임시
    @GetMapping("/login")
    public ApiResponse<String> login(
            @RequestParam(defaultValue = "01012341234") String phoneNumber
    ){
        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(()->new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        CustomUserDetails userDetails = new CustomUserDetails(member);

        BaseSuccessCode code = GeneralSuccessCode.OK;
        return ApiResponse.onSuccess(code, jwtUtil.createAccessToken(userDetails));
    }

    // 임시
    @GetMapping("/api-test")
    public ApiResponse<Void> apiTest(
            @AuthenticationPrincipal AuthUser user
    ) throws GeneralSecurityException {
        return ApiResponse.onSuccess(MemberSuccessCode.OK, memberService.apiTest(user));
    }

    // 내 정보 조회
    @GetMapping("/members/me")
    public ApiResponse<MemberResDTO.MemberInfo> getMemberInfo(
            @AuthenticationPrincipal AuthUser user // 임시
    ){
        BaseSuccessCode code = MemberSuccessCode.OK;
        return ApiResponse.onSuccess(code, memberService.getMemberInfo(user));
    }

    // 간편 비밀번호 변경
    @PatchMapping("/members/me/password")
    public ApiResponse<Map<String, String>> changePassword(
            @RequestBody MemberReqDTO.ChangePassword dto,
            @AuthenticationPrincipal AuthUser user // 임시
    ){
        Optional<Map<String, String>> result = memberService.changePassword(dto, user);
        if (result.isPresent()){
            BaseErrorCode code = GeneralErrorCode.VALIDATION_FAILED;
            return ApiResponse.onFailure(code, result.get());
        } else {
            BaseSuccessCode code = MemberSuccessCode.CHANGE_SIMPLE_PASSWORD;
            return ApiResponse.onSuccess(code, null);
        }
    }

    // 간편 비밀번호 재설정
    @PostMapping("/members/me/password/reset")
    public ApiResponse<Void> resetPassword(
            @Validated @RequestBody MemberReqDTO.ResetPassword dto,
            @AuthenticationPrincipal AuthUser user // 임시
    ){
        BaseSuccessCode code = MemberSuccessCode.RESET_SIMPLE_PASSWORD;
        return ApiResponse.onSuccess(code, memberService.resetPassword(dto, user));
    }

    // 거래소 목록 조회
    @GetMapping("/exchanges")
    public ApiResponse<List<MemberResDTO.ExchangeList>> getExchangeList(
            @AuthenticationPrincipal AuthUser user
    ){
        BaseSuccessCode code = MemberSuccessCode.EXCHANGE_LIST;
        return ApiResponse.onSuccess(code, memberService.getExchangeList(user));
    }

    // API키 목록 조회
    @GetMapping("/members/me/api-keys")
    public ApiResponse<List<MemberResDTO.ApiKeyList>> getApiKeyList(
            @AuthenticationPrincipal AuthUser user
    ){
        BaseSuccessCode code = MemberSuccessCode.GET_API_KEY_LIST;
        List<MemberResDTO.ApiKeyList> result = memberService.getApiKeyList(user);
        if (result.isEmpty()){
            result = null;
        }
        return ApiResponse.onSuccess(code, result);
    }

    // API키 등록 및 수정
    @PostMapping("/members/me/api-keys")
    public ApiResponse<List<String>> postPatchApiKey(
            @AuthenticationPrincipal AuthUser user,
            @RequestBody List<MemberReqDTO.PostPatchApiKey> dto
    ){
        BaseSuccessCode code = MemberSuccessCode.POST_PATCH_API_KEY;
        List<String> result = memberService.postPatchApiKey(user, dto);
        if (result.isEmpty()){
            result = null;
        }
        return ApiResponse.onSuccess(code, result);
    }

    // API키 삭제
    @DeleteMapping("/members/me/api-keys")
    public ApiResponse<Void> deleteApiKey(
            @AuthenticationPrincipal AuthUser user,
            @RequestBody MemberReqDTO.DeleteApiKey dto
    ){
        BaseSuccessCode code = MemberSuccessCode.DELETE_API_KEY;
        return ApiResponse.onSuccess(code, memberService.deleteApiKey(user, dto));
    }

    // FCM 토큰 등록
    @PostMapping("/members/me/fcm")
    public ApiResponse<Void> postFcmToken(
            @AuthenticationPrincipal AuthUser user,
            @RequestBody MemberReqDTO.PostFcmToken dto
    ){
        BaseSuccessCode code = MemberSuccessCode.POST_PATCH_FCM_TOKEN;
        return ApiResponse.onSuccess(code, memberService.postFcmToken(user, dto));
    }
}
