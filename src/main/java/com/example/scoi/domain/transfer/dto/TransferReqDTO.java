package com.example.scoi.domain.transfer.dto;

import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.member.enums.MemberType;
import com.example.scoi.domain.transfer.enums.CoinType;
import com.example.scoi.domain.transfer.enums.NetworkType;

public class TransferReqDTO {

    public record RecipientInformation(
            MemberType memberType,
            String recipientName,
            String corpKoreanName,
            String corpEnglishName,
            String walletAddress,
            ExchangeType exchangeType,
            CoinType coinType,
            String netType
    ) { }

    public record Quote(
            String available,
            String amount,
            CoinType coinType,
            NetworkType networkType,
            String networkFee
    ) {}


    ) { }
}
