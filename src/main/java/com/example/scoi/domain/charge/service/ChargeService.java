package com.example.scoi.domain.charge.service;

import com.example.scoi.global.client.adapter.BithumbAdapter;
import com.example.scoi.global.client.adapter.UpbitAdapter;
import com.example.scoi.global.client.adapter.ExchangeApiClient;
import com.example.scoi.domain.charge.dto.ChargeResDTO;
import com.example.scoi.domain.charge.exception.ChargeException;
import com.example.scoi.domain.charge.exception.code.ChargeErrorCode;
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
public class ChargeService {

    private final MemberRepository memberRepository;
    private final BithumbAdapter bithumbAdapter;
    private final UpbitAdapter upbitAdapter;

    /**
     * 보유 자산 조회
     * 지금 버전: Member 조회 후 phoneNumber 사용
     * dev로 최신화 후 병합하면 AuthUser 구현 후 파라미터를 AuthUser로 변경 예정
     */
    public ChargeResDTO.BalanceDTO getBalances(Long memberId, ExchangeType exchangeType) {
        // 사용자의 API 키를 DB에서 가져오기 (Member 조회하여 phoneNumber 가져오기)
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ChargeException(ChargeErrorCode.API_KEY_NOT_FOUND));
        
        String phoneNumber = member.getPhoneNumber();

        // 시크릿 키 복호화하기 -JwtApiUtil 
        //쿼리 파라미터에 따라 빗썸 /업비트 API 조회하기
        ExchangeApiClient apiClient = getApiClient(exchangeType);
        
        try {//응답
            return apiClient.getBalance(phoneNumber, exchangeType);
        } catch (Exception e) {
            log.error("거래소 API 호출 실패 - exchangeType: {}, phoneNumber: {}, error: {}", 
                    exchangeType, phoneNumber, e.getMessage(), e);
            throw new ChargeException(ChargeErrorCode.EXCHANGE_API_ERROR);
        }
    }

    private ExchangeApiClient getApiClient(ExchangeType exchangeType) {
        return switch (exchangeType) {
            case BITHUMB -> bithumbAdapter;
            case UPBIT -> upbitAdapter;
            case BINANCE -> throw new ChargeException(ChargeErrorCode.INVALID_EXCHANGE_TYPE); // 바이낸스는 후순위
        };
    }
}
