package com.example.scoi.global.client.adapter;

import com.example.scoi.domain.charge.dto.BalanceResDTO;
import com.example.scoi.domain.member.enums.ExchangeType;

/**
 * 거래소 API 클라이언트 공통 인터페이스
 * - 모든 거래소 Adapter가 구현해야 하는 공통 인터페이스
 * - Service에서 ExchangeType에 따라 적절한 Adapter를 선택
 * - Adapter- 거래소별 인증, API 호출, 응답 변환을 담당
 */
public interface ExchangeApiClient {

    /**
     * 보유 자산 조회
     * 
     * @param phoneNumber 사용자 휴대전화 번호 (API 키 조회용)
     * @param exchangeType 거래소 타입
     * @return 표준화된 BalanceDTO
     */
    BalanceResDTO.BalanceDTO getBalance(String phoneNumber, ExchangeType exchangeType);
}