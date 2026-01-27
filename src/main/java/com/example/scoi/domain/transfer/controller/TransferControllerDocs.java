package com.example.scoi.domain.transfer.controller;

import com.example.scoi.domain.transfer.dto.TransferReqDTO;
import com.example.scoi.domain.transfer.dto.TransferResDTO;
import com.example.scoi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface TransferControllerDocs {
    @Operation(
            summary = "최근 수취인 목록 조회 API By 김민규",
            description = "최근 거래한 수취인 목록을 커서 기반 무한스크롤 방식으로 조회합니다. 최신순으로 정렬됩니다.")
    ApiResponse<TransferResDTO.RecipientListDTO> getRecentRecipients(
            @AuthenticationPrincipal String phoneNumber,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", defaultValue = "3") int limit
    );

    @Operation(
            summary = "즐겨찾기 수취인 목록 조회 API By 김민규",
            description = "즐겨찾기 수취인 목록을 커서 기반 무한스크롤 방식으로 조회합니다. 최신순으로 정렬됩니다.")
    ApiResponse<TransferResDTO.RecipientListDTO> getFavoriteRecipients(
            @AuthenticationPrincipal String phoneNumber,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", defaultValue = "3") int limit
    );

    @Operation(
            summary = "즐겨찾기 수취인 등록 API By 김민규",
            description = "수취인의 정보(성명, 지갑주소, 거래소, 맴버타입(개인/기업)을 입력하여 즐겨찾기 수취인을 등록합니다.")
    ApiResponse<Long> addFavoriteRecipient(
            @AuthenticationPrincipal String phoneNumber,
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
}
