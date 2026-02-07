package com.example.scoi.domain.transfer.controller;

import com.example.scoi.domain.transfer.dto.TransferReqDTO;
import com.example.scoi.domain.transfer.dto.TransferResDTO;
import com.example.scoi.domain.transfer.exception.code.TransferSuccessCode;
import com.example.scoi.global.apiPayload.ApiResponse;
import com.example.scoi.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "이체 API")
public interface TransferControllerDocs {
    @Operation(
            summary = "최근 수취인 목록 조회 API By 김민규",
            description = "최근 거래한 수취인 목록을 커서 기반 무한스크롤 방식으로 조회합니다. 최신순으로 정렬됩니다.")
    ApiResponse<TransferResDTO.RecipientListDTO> getRecentRecipients(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", defaultValue = "3") int limit
    );

    @Operation(
            summary = "즐겨찾기 수취인 목록 조회 API By 김민규",
            description = "즐겨찾기 수취인 목록을 커서 기반 무한스크롤 방식으로 조회합니다. 최신순으로 정렬됩니다.")
    ApiResponse<TransferResDTO.RecipientListDTO> getFavoriteRecipients(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", defaultValue = "3") int limit
    );

    @Operation(
            summary = "즐겨찾기 수취인 등록 API By 김민규",
            description = "수취인의 정보(성명, 지갑주소, 거래소, 맴버타입(개인/기업)을 입력하여 즐겨찾기 수취인을 등록합니다.")
    ApiResponse<Long> addFavoriteRecipient(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody TransferReqDTO.RecipientInformation recipientInformation
    );

    @Operation(
            summary = "별 모양 클릭 시 즐겨찾기 수취인으로 등록 API By 김민규",
            description = "최근 거래 내역에서 별모양을 눌러 즐겨찾기 수취인으로 등록합니다.")
    ApiResponse<Long> changeToFavorite(@PathVariable Long recipientId);

    @Operation(
            summary = "별 모양 클릭 시 즐겨찾기 수취인으로 해제 API By 김민규",
            description = "최근 거래 내역에서 별모양을 눌러 즐겨찾기 수취인으로 등록을 해제합니다.")
    ApiResponse<Long> changeToNotFavorite(@PathVariable Long recipientId);

    @Operation(
            summary = "입력받은 수취인 값을 검증 API By 김민규",
            description = "수취인의 지갑주소 형식, 법인인데 법인 정보가 없는 경우를 검증하고 거래소 API를 호출해 사용자 정보와 내 지갑의 출금 가능 잔액을 반환합니다.")
    ApiResponse<TransferResDTO.CheckRecipientResDTO> checkRecipientInput(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody TransferReqDTO.RecipientInformation recipientInformation
    );

    @Operation(
            summary = "출금 견적을 검증 API By 김민규",
            description = "사용자가 입력한 출금 값 + 네트워크 수수료가 남은 잔고보다 많은지 검증하고 그 값들을 반환합니다.")
    ApiResponse<TransferResDTO.QuoteValidDTO> checkQuotes(
            @RequestBody TransferReqDTO.Quote quotes
    );

    @Operation(
            summary = "출금을 실행 API By 김민규",
            description = "사용자의 간편 비밀번호를 확인한 후, 지정된 거래소로 코인을 이체합니다.")
    ApiResponse<TransferResDTO.WithdrawResult> executeWithdraw(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody TransferReqDTO.WithdrawRequest request
    );
}
