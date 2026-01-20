package com.example.scoi.domain.transfer.converter;

import com.example.scoi.domain.transfer.dto.TransferResDTO;
import com.example.scoi.domain.transfer.entity.Recipient;
import com.example.scoi.domain.transfer.entity.TradeHistory;
import com.example.scoi.domain.transfer.enums.CoinType;

import java.util.List;
import java.util.stream.Collectors;

public class TransferConverter {

    // 전체 리스트 및 페이징 정보 변환
    public static TransferResDTO.RecipientListDTO toRecentRecipientListDTO(
            List<TradeHistory> histories, String nextCursor, boolean hasNext) {

        List<TransferResDTO.RecipientDTO> items = histories.stream()
                .map(TransferConverter::toRecentRecipientDTO)
                .collect(Collectors.toList());

        return TransferResDTO.RecipientListDTO.builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    // 단일 엔티티 -> DTO 변환
    public static TransferResDTO.RecipientDTO toRecentRecipientDTO(TradeHistory history) {
        Recipient recipient = history.getRecipient();

        return TransferResDTO.RecipientDTO.builder()
                .recipientId(recipient.getId())
                .recipientType(recipient.getRecipientType())
                .recipientName(recipient.getRecipientKoName())
                .corpKoreanName(recipient.getRecipientCorpKoName())
                .corpEnglishName(recipient.getRecipientCorpEnName())
                .walletAddress(recipient.getWalletAddress())
                .exchangeType(history.getExchangeType())
                .network(resolveNetwork(history.getCoinType())) // 네트워크 변환
                .isFavorite(recipient.getIsFavorite())
                .build();
    }

    // USDT -> TRON, USDC -> ETHERIUM 고정
    private static String resolveNetwork(CoinType coinType) {
        if(coinType.equals(CoinType.USDT)) return "TRON";
        if(coinType.equals(CoinType.USDC)) return "ETHEREUM";
        return null;
    }
}
