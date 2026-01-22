package com.example.scoi.domain.transfer.controller;

import com.example.scoi.domain.member.entity.Member;
import com.example.scoi.domain.transfer.dto.TransferReqDTO;
import com.example.scoi.domain.transfer.dto.TransferResDTO;
import com.example.scoi.domain.transfer.exception.code.TransferSuccessCode;
import com.example.scoi.domain.transfer.service.TransferService;
import com.example.scoi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @Operation(
            summary = "최근 수취인 목록 조회 API By 김민규",
            description = "최근 거래한 수취인 목록을 커서 기반 무한스크롤 방식으로 조회합니다. 최신순으로 정렬됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "TRANSFER200_1", description = "최근 수취인 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "TRANSFER404_1", description = "현재 사용중인 사용자를 찾을 수 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "TRANSFER400_1", description = "cursor가 올바르지 않습니다.")
    })
    @GetMapping("/recipients/recent")
    public ApiResponse<TransferResDTO.RecipientListDTO> getRecentRecipients(
            @AuthenticationPrincipal Member member,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", defaultValue = "3") int limit
    ){

        return ApiResponse.onSuccess(
                TransferSuccessCode.TRANSFER200_1, transferService.findRecentRecipients(member, cursor, limit));
    }

    @Operation(
            summary = "즐겨찾기 수취인 목록 조회 API By 김민규",
            description = "즐겨찾기 수취인 목록을 커서 기반 무한스크롤 방식으로 조회합니다. 최신순으로 정렬됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "TRANSFER200_2", description = "즐겨찾기 수취인 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "TRANSFER404_1", description = "현재 사용중인 사용자를 찾을 수 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "TRANSFER400_1", description = "cursor가 올바르지 않습니다.")
    })
    @GetMapping("/recipients/favorites")
    public ApiResponse<TransferResDTO.RecipientListDTO> getFavoriteRecipients(
            @AuthenticationPrincipal Member member,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", defaultValue = "3") int limit
    ){

        return ApiResponse.onSuccess(
                TransferSuccessCode.TRANSFER200_2, transferService.findFavoriteRecipients(member, cursor, limit));
    }

    @Operation(
            summary = "즐겨찾기 수취인 등록 API By 김민규",
            description = "수취인의 정보(성명, 지갑주소, 거래소, 맴버타입(개인/기업)을 입력하여 즐겨찾기 수취인을 등록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "TRANSFER201_1", description = "즐겨찾기 수취인 등록에 성공했습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "TRANSFER404_1", description = "현재 사용중인 사용자를 찾을 수 없습니다.")
    })
    @PostMapping("/recipients/favorites")
    public ApiResponse<Long> addFavoriteRecipient(
            @AuthenticationPrincipal Member member,
            @RequestBody TransferReqDTO.RecipientInformation recipientInformation
            ){

        return ApiResponse.onSuccess(
                TransferSuccessCode.TRANSFER201_1, transferService.addFavoriteRecipient(member, recipientInformation));
    }

    @Operation(
            summary = "별 모양 클릭 시 즐겨찾기 수취인으로 등록 API By 김민규",
            description = "최근 거래 내역에서 별모양을 눌러 즐겨찾기 수취인으로 등록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "TRANSFER200_3", description = "즐겨찾기 수취인 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "TRANSFER404_1", description = "현재 사용중인 사용자를 찾을 수 없습니다.")
    })
    @PostMapping("/recipients/favorites/{recipientId}/register")
    public ApiResponse<Long> changeToFavorite(
            @PathVariable Long recipientId
    ){
        return ApiResponse.onSuccess(TransferSuccessCode.TRANSFER200_3,
                transferService.changeToFavoriteRecipient(recipientId));
    }

    @Operation(
            summary = "별 모양 클릭 시 즐겨찾기 수취인으로 해제 API By 김민규",
            description = "최근 거래 내역에서 별모양을 눌러 즐겨찾기 수취인으로 등록을 해제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "TRANSFER200_4", description = "즐겨찾기 수취인 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "TRANSFER404_1", description = "현재 사용중인 사용자를 찾을 수 없습니다.")
    })
    @PostMapping("/recipients/favorites/{recipientId}/unregister")
    public ApiResponse<Long> changeToNotFavorite(
            @PathVariable Long recipientId
    ){
        return ApiResponse.onSuccess(TransferSuccessCode.TRANSFER200_4,
                transferService.changeToNotFavoriteRecipient(recipientId));
    }
}
