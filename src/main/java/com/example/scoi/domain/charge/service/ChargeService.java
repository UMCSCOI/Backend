package com.example.scoi.domain.charge.service;

import com.example.scoi.domain.charge.client.BithumbApiClient;
import com.example.scoi.domain.charge.client.UpbitApiClient;
import com.example.scoi.domain.charge.client.ExchangeApiClient;
import com.example.scoi.domain.charge.dto.ChargeResDTO;
import com.example.scoi.domain.charge.exception.ChargeException;
import com.example.scoi.domain.charge.exception.code.ChargeErrorCode;
import com.example.scoi.domain.member.entity.Member;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChargeService {

    private final MemberRepository memberRepository;
    private final BithumbApiClient bithumbApiClient;
    private final UpbitApiClient upbitApiClient;

    /**
     * 정상 버전: Member 조회 후 phoneNumber 사용
     * 로직은 노션에 있는 API 명세서 기준.
     */
    public ChargeResDTO.BalanceDTO getBalances(Long memberId, ExchangeType exchangeType) {
        // 로직 1: 사용자의 API 키를 DB에서 가져오기 (Member 조회하여 phoneNumber 가져오기)
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ChargeException(ChargeErrorCode.API_KEY_NOT_FOUND));
        
        String phoneNumber = member.getPhoneNumber();

        // 로직 2: 시크릿 키 복호화하기 (JwtApiUtil 내부에서 처리)
        // 로직 3: 쿼리 파라미터에 따라 빗썸 or 업비트 API 조회하기
        ExchangeApiClient apiClient = getApiClient(exchangeType);
        
        try {
            // 로직 4: 빗썸, 업비트 서버 응답 정제해 보내기
            return apiClient.getBalance(phoneNumber, exchangeType);
        } catch (Exception e) {
            throw new ChargeException(ChargeErrorCode.EXCHANGE_API_ERROR);
        }
    }

    /**
     * 임시 테스트용: phoneNumber 직접 사용 (Member 조회 생략) => dev 최신화되면 삭제 예정정
     * 위 메서드 주석 처리하고 이 메서드 주석 해제하여 사용
     */
    /*
    public ChargeResDTO.BalanceDTO getBalances(String phoneNumber, ExchangeType exchangeType) {
        // 임시 테스트용: Member 조회 생략, phoneNumber 직접 사용
        
        // 로직 2: 시크릿 키 복호화하기 (JwtApiUtil 내부에서 처리)
        // 로직 3: 쿼리 파라미터에 따라 빗썸 or 업비트 API 조회하기
        ExchangeApiClient apiClient = getApiClient(exchangeType);
        
        try {
            // 로직 4: 빗썸, 업비트 서버 응답 정제해 보내기
            return apiClient.getBalance(phoneNumber, exchangeType);
        } catch (Exception e) {
            throw new ChargeException(ChargeErrorCode.EXCHANGE_API_ERROR);
        }
    }
    */

    private ExchangeApiClient getApiClient(ExchangeType exchangeType) {
        return switch (exchangeType) {
            case BITHUMB -> bithumbApiClient;
            case UPBIT -> upbitApiClient;
            case BINANCE -> throw new ChargeException(ChargeErrorCode.INVALID_EXCHANGE_TYPE);
        };
    }
}
