package com.example.scoi.domain.transfer.dto;

import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.member.enums.MemberType;

public class TransferReqDTO {

    public record RecipientInformation(
            MemberType memberType,
            String recipientName,
            String corpKoreanName,
            String corpEnglishName,
            String walletAddress,
            ExchangeType exchangeType

    ) { }
}
