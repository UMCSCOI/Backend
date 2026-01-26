package com.example.scoi.domain.transfer.controller;

import com.example.scoi.domain.transfer.dto.TransferReqDTO;
import com.example.scoi.domain.transfer.dto.TransferResDTO;
import com.example.scoi.domain.transfer.exception.code.TransferSuccessCode;
import com.example.scoi.domain.transfer.service.TransferService;
import com.example.scoi.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController implements TransferControllerDocs{

    private final TransferService transferService;

    @GetMapping("/recipients/recent")
    public ApiResponse<TransferResDTO.RecipientListDTO> getRecentRecipients(
            @AuthenticationPrincipal String phoneNumber,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", defaultValue = "3") int limit
    ){

        return ApiResponse.onSuccess(
                TransferSuccessCode.TRANSFER200_1, transferService.findRecentRecipients(phoneNumber, cursor, limit));
    }

    @GetMapping("/recipients/favorites")
    public ApiResponse<TransferResDTO.RecipientListDTO> getFavoriteRecipients(
            @AuthenticationPrincipal String phoneNumber,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", defaultValue = "3") int limit
    ){

        return ApiResponse.onSuccess(
                TransferSuccessCode.TRANSFER200_2, transferService.findFavoriteRecipients(phoneNumber, cursor, limit));
    }

    @PostMapping("/recipients/favorites")
    public ApiResponse<Long> addFavoriteRecipient(
            @AuthenticationPrincipal String phoneNumber,
            @RequestBody TransferReqDTO.RecipientInformation recipientInformation
            ){

        return ApiResponse.onSuccess(
                TransferSuccessCode.TRANSFER201_1, transferService.addFavoriteRecipient(phoneNumber, recipientInformation));
    }

    @PostMapping("/recipients/favorites/{recipientId}/register")
    public ApiResponse<Long> changeToFavorite(
            @PathVariable Long recipientId
    ){
        return ApiResponse.onSuccess(TransferSuccessCode.TRANSFER200_3,
                transferService.changeToFavoriteRecipient(recipientId));
    }

    @PostMapping("/recipients/favorites/{recipientId}/unregister")
    public ApiResponse<Long> changeToNotFavorite(
            @PathVariable Long recipientId
    ){
        return ApiResponse.onSuccess(TransferSuccessCode.TRANSFER200_4,
                transferService.changeToNotFavoriteRecipient(recipientId));
    }
}
