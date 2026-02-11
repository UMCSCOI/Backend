package com.example.scoi.domain.transfer.converter;

import com.example.scoi.domain.member.entity.Member;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.member.enums.MemberType;
import com.example.scoi.domain.transfer.dto.TransferReqDTO;
import com.example.scoi.domain.transfer.dto.TransferResDTO;
import com.example.scoi.domain.transfer.entity.Recipient;
import com.example.scoi.domain.transfer.entity.TradeHistory;
import com.example.scoi.domain.transfer.enums.CoinType;
import com.example.scoi.domain.transfer.enums.NetworkType;
import com.example.scoi.domain.transfer.enums.TradeType;
import com.example.scoi.global.client.dto.BithumbResDTO;
import com.example.scoi.global.client.dto.UpbitResDTO;

import java.time.LocalDateTime;
import java.util.Collections;
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
                .recipientKoName(recipient.recipientKoName())
                .recipientType(MemberType.from(recipient.memberType()))
//                .recipientCorpKoName(recipient.corpKoreanName())
//                .recipientCorpEnName(recipient.corpEnglishName())
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
                .recipientType(MemberType.from(info.memberType()))
                .recipientKoName(info.recipientKoName())
                .recipientEnName(info.recipientEnName())
//                .corpKoreanName(info.corpKoreanName())
//                .corpEnglishName(info.corpEnglishName())
                .walletAddress(info.walletAddress())
                .build();
    }

    // dto로 변환
    public static TransferResDTO.QuoteValidDTO toQuoteValidDTO(String amount, String networkFee, String totalAmount) {
        return TransferResDTO.QuoteValidDTO.builder()
                .amount(amount)
                .networkFee(networkFee)
                .totalAmount(totalAmount)
                .build();
    }

    // 업비트 요청으로 변환
    public static TransferReqDTO.UpbitWithdrawRequest toUpbitWithdrawRequest(TransferReqDTO.WithdrawRequest dto){
        return new TransferReqDTO.UpbitWithdrawRequest(
                dto.currency(),
                dto.netType(),
                dto.amount(),
                dto.address()
        );
    }

    // 빗썸 요청으로 변환
    public static TransferReqDTO.BithumbWithdrawRequest toBithumbWithdrawRequest(TransferReqDTO.WithdrawRequest dto) {

        String mappedReceiverType = dto.receiverType().equals("INDIVIDUAL") ? "personal" : "corporation";

        return TransferReqDTO.BithumbWithdrawRequest.builder()
                .currency(dto.currency())
                .netType(dto.netType())
                .amount(Double.valueOf(dto.amount()))
                .address(dto.address())
                .exchangeName(String.valueOf(dto.exchangeName()))
                .receiverType(mappedReceiverType)
                .receiverKoName(dto.receiverKoName())
                .receiverEnName(dto.receiverEnName())
//                .receiverCorpKoName(dto.receiverCorpKoName()) // 법인일 때만 값이 들어있음
//                .receiverCorpEnName(dto.receiverCorpEnName())
                .build();
    }

    // 1. 업비트용 컨버터 (외부 호출용)
    public static TransferResDTO.WithdrawResult toWithdrawResult(UpbitResDTO.WithdrawResDTO upbitRes) {
        return buildWithdrawResult(
                upbitRes.amount(),
                upbitRes.currency(),
                upbitRes.uuid(),
                upbitRes.createdAt(),
                upbitRes.state(),
                upbitRes.fee()
        );
    }
    // 2. 빗썸용 컨버터 (외부 호출용)
    public static TransferResDTO.WithdrawResult toWithdrawResult(BithumbResDTO.WithdrawResDTO bithumbRes) {
        return buildWithdrawResult(
                bithumbRes.amount(),
                bithumbRes.currency(),
                bithumbRes.uuid(),
                bithumbRes.createdAt(),
                bithumbRes.state(),
                bithumbRes.fee()
        );
    }
    // 3. 빌더 로직 통합 (내부 공통 메서드)
    private static TransferResDTO.WithdrawResult buildWithdrawResult(
            String amount, String currency, String uuid, String createdAtStr, String state, String fee
    ) {
        String totalAmount = calculateTotalAmount(amount, fee);

        LocalDateTime createdAt = (createdAtStr == null)
                ? LocalDateTime.now() // 값이 없으면 현재 시간으로 대체
                : LocalDateTime.parse(createdAtStr);
        return TransferResDTO.WithdrawResult.builder()
                .amount(totalAmount)
                .currency(currency)
                .uuid(uuid)
                .state(state)
                .createdAt(String.valueOf(createdAt))
                .build();
    }

    public static Recipient toRecipient(TransferReqDTO.WithdrawRequest request, Member member){
        return Recipient.builder()
                .walletAddress(request.address())
                .recipientEnName(request.receiverEnName())
                .recipientKoName(request.receiverKoName())
                .recipientType(MemberType.from(request.receiverType()))
                .member(member)
                .build();
    }

    public static TradeHistory toTradeHistory(TransferReqDTO.WithdrawRequest request, TransferResDTO.WithdrawResult result, Recipient recipient, Member member) {

        LocalDateTime createdAt = (result.getCreatedAt() == null)
                ? LocalDateTime.now() // 값이 없으면 현재 시간으로 대체
                : LocalDateTime.parse(result.getCreatedAt());

        return TradeHistory.builder()
                .exchangeType(request.exchangeName())
                .coinCount(result.getAmount())
                .createdAt(createdAt)
                .coinType(CoinType.valueOf(result.getCurrency()))
                .status(result.getState())
                .recipient(recipient)
                .tradeType(TradeType.WITHDRAWAL)
                .idempotentKey(request.idempotentKey())
                .member(member)
                .uuid(result.getUuid())
                .build();
    }

    public static TransferResDTO.WithdrawResult toWithdrawResult(TradeHistory tradeHistory) {
        return TransferResDTO.WithdrawResult.builder()
                .amount(tradeHistory.getCoinCount())
                .currency(String.valueOf(tradeHistory.getCoinType()))
                .uuid(tradeHistory.getUuid())
                .createdAt(String.valueOf(tradeHistory.getCreatedAt()))
                .state(tradeHistory.getStatus())
                .build();
    }

    // 수수료와 출금값 더해주는 메서드
    private static String calculateTotalAmount(String amountStr, String feeStr) {
        try {
            // 정확한 금액 계산을 위해 BigDecimal 권장 (Double은 부동소수점 오차 발생 가능)
            java.math.BigDecimal amount = new java.math.BigDecimal(amountStr != null ? amountStr : "0");
            java.math.BigDecimal fee = new java.math.BigDecimal(feeStr != null ? feeStr : "0");

            return amount.add(fee).toPlainString();
        } catch (NumberFormatException e) {
            // 파싱 실패 시 기본값 반환 혹은 에러 처리
            return amountStr;
        }
    }

    public static List<TransferResDTO.WithdrawRecipients> toWithdrawRecipientsUpbit(List<UpbitResDTO.WithdrawalAddressResponse> upbitResult, CoinType coinType) {
        // 수취인이 없는 경우 빈 리스트 반환
        if (upbitResult == null) {
            return Collections.emptyList();
        }

        return upbitResult.stream()
                .filter(item -> item.currency().equals(String.valueOf(coinType)))
                .map(item -> TransferResDTO.WithdrawRecipients.builder()
                        .memberType(MemberType.from(item.beneficiary_type()))
                        .recipientKoName(item.beneficiary_name())
                        .recipientEnName(null)
                        .walletAddress(item.withdraw_address())
                        .exchangeType(ExchangeType.fromString(item.exchange_name().toUpperCase()))
                        .currency(CoinType.valueOf(item.currency()))
                        .netType(NetworkType.valueOf(item.net_type()))
                        .build()).toList();
    }
    public static List<TransferResDTO.WithdrawRecipients> toWithdrawRecipientsBithumb(List<BithumbResDTO.WithdrawalAddressResponse> bithumbResult, CoinType coinType) {
        // 수취인이 없는 경우 빈 리스트 반환
        if (bithumbResult == null) {
            return Collections.emptyList();
        }

        return bithumbResult.stream()
                .filter(item -> item.currency().equals(String.valueOf(coinType)))
                .map(item -> TransferResDTO.WithdrawRecipients.builder()
                        .memberType(MemberType.from(item.owner_type()))
                        .recipientKoName(item.owner_ko_name())
                        .recipientEnName(item.owner_en_name())
                        .walletAddress(item.withdraw_address())
                        .exchangeType(ExchangeType.fromString(item.exchange_name().toUpperCase()))
                        .currency(CoinType.valueOf(item.currency()))
                        .netType(NetworkType.valueOf(item.net_type()))
                        .build()).toList();
    }
}
