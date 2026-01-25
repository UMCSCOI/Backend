package com.example.scoi.domain.transfer.converter;

import com.example.scoi.domain.member.entity.Member;
import com.example.scoi.domain.transfer.dto.TransferReqDTO;
import com.example.scoi.domain.transfer.dto.TransferResDTO;
import com.example.scoi.domain.transfer.entity.Recipient;
import com.example.scoi.domain.transfer.entity.TradeHistory;

import java.util.List;
import java.util.stream.Collectors;

public class TransferConverter {

    // 전체 리스트 및 페이징 정보 변환
    public static TransferResDTO.RecipientListDTO toRecentRecipientListDTO(
            List<TradeHistory> histories, String nextCursor, boolean hasNext) {

        List<TransferResDTO.RecipientDTO> items = histories.stream()
                .map(TradeHistory::getRecipient)
                .map(TransferConverter::toRecentRecipientDTO) // recipient 넘겨주기
                .toList();

        return TransferResDTO.RecipientListDTO.builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    // 단일 엔티티 -> DTO 변환
    public static TransferResDTO.RecipientDTO toRecentRecipientDTO(Recipient recipient) {
        return TransferResDTO.RecipientDTO.builder()
                .recipientId(recipient.getId())
                .recipientType(recipient.getRecipientType())
                .recipientName(recipient.getRecipientKoName())
                .corpKoreanName(recipient.getRecipientCorpKoName())
                .corpEnglishName(recipient.getRecipientCorpEnName())
                .walletAddress(recipient.getWalletAddress())
                .isFavorite(recipient.getIsFavorite())
                .build();
    }

    public static TransferResDTO.RecipientListDTO toFavoriteRecipientListDTO(
            List<Recipient> recipients, String nextCursor, boolean hasNext
    ){
        List<TransferResDTO.RecipientDTO> items = recipients.stream()
                .map(TransferConverter::toFavoriteRecipientDTO)
                .collect(Collectors.toList());

        return TransferResDTO.RecipientListDTO.builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    public static TransferResDTO.RecipientDTO toFavoriteRecipientDTO(Recipient recipient){
        return TransferResDTO.RecipientDTO.builder()
                .recipientId(recipient.getId())
                .recipientType(recipient.getRecipientType())
                .recipientName(recipient.getRecipientKoName())
                .isFavorite(recipient.getIsFavorite())
                .build();
    }

    // dto -> 객체
    public static Recipient toFavoriteRecipient(TransferReqDTO.RecipientInformation recipient, Member member){
        return Recipient.builder()
                .walletAddress(recipient.walletAddress())
                .recipientKoName(recipient.recipientName())
                .recipientType(recipient.memberType())
                .recipientCorpKoName(recipient.corpKoreanName())
                .recipientCorpEnName(recipient.corpEnglishName())
                .isFavorite(true)
                .member(member)
                .build();
    }
}
