package com.example.scoi.domain.transfer.converter;

import com.example.scoi.domain.member.entity.Member;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.transfer.dto.TransferReqDTO;
import com.example.scoi.domain.transfer.dto.TransferResDTO;
import com.example.scoi.domain.transfer.entity.Recipient;
import com.example.scoi.domain.transfer.entity.TradeHistory;
import com.example.scoi.global.client.dto.BithumbResDTO;
import com.example.scoi.global.client.dto.UpbitResDTO;

import java.time.LocalDateTime;
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

    // 1. 업비트용 컨버터
    public static TransferResDTO.CheckRecipientResDTO toCheckRecipientResDTO(
            TransferReqDTO.RecipientInformation info,
            UpbitResDTO.WithdrawsChance upbitResult // 매개변수 타입이 다름
    ) {
        return TransferResDTO.CheckRecipientResDTO.builder()
                .recipient(toRecipientDetailDTO(info))
                .balance(TransferResDTO.BalanceDTO.builder()
                        .exchangeType(ExchangeType.UPBIT)
                        .coinType(info.coinType())
                        .network(info.netType())
                        .networkFee(upbitResult.currency().withdraw_fee())
                        .availableAmount(upbitResult.account().balance())
                        .updatedAt(LocalDateTime.now().toString())
                        .build())
                .build();
    }
    // 2. 빗썸용 컨버터
    public static TransferResDTO.CheckRecipientResDTO toCheckRecipientResDTO(
            TransferReqDTO.RecipientInformation info,
            BithumbResDTO.WithdrawsChance bithumbResult // 매개변수 타입이 다름
    ) {
        return TransferResDTO.CheckRecipientResDTO.builder()
                .recipient(toRecipientDetailDTO(info))
                .balance(TransferResDTO.BalanceDTO.builder()
                        .exchangeType(ExchangeType.BITHUMB)
                        .coinType(info.coinType())
                        .network(info.netType())
                        .networkFee(bithumbResult.currency().withdraw_fee())
                        .availableAmount(bithumbResult.account().balance())
                        .updatedAt(LocalDateTime.now().toString())
                        .build())
                .build();
    }

    // 공통 수취인 정보 변환 로직 (중복 제거)
    private static TransferResDTO.RecipientDetailDTO toRecipientDetailDTO(TransferReqDTO.RecipientInformation info) {
        return TransferResDTO.RecipientDetailDTO.builder()
                .recipientType(info.memberType())
                .recipientName(info.recipientName())
                .corpKoreanName(info.corpKoreanName())
                .corpEnglishName(info.corpEnglishName())
                .walletAddress(info.walletAddress())
                .build();
    }

}
