package com.example.scoi.domain.invest.service;

import com.example.scoi.domain.invest.client.BithumbApiClient;
import com.example.scoi.domain.invest.client.UpbitApiClient;
import com.example.scoi.domain.invest.client.BinanceApiClient;
import com.example.scoi.domain.invest.client.ExchangeApiClient;
import com.example.scoi.domain.invest.dto.MaxOrderInfoDTO;
import com.example.scoi.domain.invest.exception.InvestException;
import com.example.scoi.domain.invest.exception.code.InvestErrorCode;
import com.example.scoi.domain.member.entity.Member;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class InvestService {

    private final MemberRepository memberRepository;
    private final BithumbApiClient bithumbApiClient;
    private final UpbitApiClient upbitApiClient;
    private final BinanceApiClient binanceApiClient;
//거래소 클라이언트 API 구현 완료 필요 -수정 단계!!
   //최대 주문 개수 조회
    
    public MaxOrderInfoDTO getMaxOrderInfo(Long memberId, ExchangeType exchangeType, String coinType) {
        // 사용자의 API 키를 DB에서 가져오기 (Member 조회하여 phoneNumber 가져오기)
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new InvestException(InvestErrorCode.API_KEY_NOT_FOUND));
        
        String phoneNumber = member.getPhoneNumber();

        //  시크릿 키 복호화하기 (JwtApiUtil 내부에서 처리)
        //  쿼리 파라미터에 따라 빗썸 or 업비트 or 바이낸스 API 조회하기
        ExchangeApiClient apiClient = getApiClient(exchangeType);
        
        try {
            // 거래소 서버 응답 정제해 보내기
            return apiClient.getMaxOrderInfo(phoneNumber, exchangeType, coinType);
        } catch (Exception e) {
            log.error("거래소 API 호출 실패 - exchangeType: {}, phoneNumber: {}, coinType: {}, error: {}", 
                    exchangeType, phoneNumber, coinType, e.getMessage(), e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }

    // 주문 가능 여부 확인
     
    public void checkOrderAvailability(
            Long memberId,
            ExchangeType exchangeType,
            String market,
            String side,
            String orderType,
            String price,
            String volume
    ) {
        // 사용자의 API 키를 DB에서 가져오기 (Member 조회하여 phoneNumber 가져오기)
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new InvestException(InvestErrorCode.API_KEY_NOT_FOUND));
        
        String phoneNumber = member.getPhoneNumber();

        // 시크릿 키 복호화하기
        // 쿼리 파라미터에 따라 빗썸 or 업비트 or 바이낸스 API 조회하기
        ExchangeApiClient apiClient = getApiClient(exchangeType);
        
        try {
            // 거래소 API 호출하여 주문 가능 여부 확인
            apiClient.checkOrderAvailability(
                    phoneNumber,
                    exchangeType,
                    market,
                    side,
                    orderType,
                    price,
                    volume
            );
        } catch (InvestException e) {
            throw e;
        } catch (Exception e) {
            log.error("거래소 API 호출 실패 - exchangeType: {}, phoneNumber: {}, market: {}, side: {}, error: {}", 
                    exchangeType, phoneNumber, market, side, e.getMessage(), e);
            throw new InvestException(InvestErrorCode.EXCHANGE_API_ERROR);
        }
    }


    private ExchangeApiClient getApiClient(ExchangeType exchangeType) {
        return switch (exchangeType) {
            case BITHUMB -> bithumbApiClient;
            case UPBIT -> upbitApiClient;
            case BINANCE -> binanceApiClient;
        };
    }
}