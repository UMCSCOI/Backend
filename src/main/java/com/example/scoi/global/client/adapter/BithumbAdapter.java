package com.example.scoi.global.client.adapter;

import com.example.scoi.domain.charge.dto.BalanceResDTO;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.global.client.converter.BithumbConverter;
import com.example.scoi.global.client.feign.BithumbFeignClient;
import com.example.scoi.global.util.JwtApiUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.GeneralSecurityException;

/**
 * 빗썸 API Adapter
 * 
 * 역할:
 * - 빗썸 API 호출을 담당하는 Adapter 구현체
 * - JWT 생성, Feign Client 호출, 응답 변환을 순차적으로 처리
 * - 거래소별 비즈니스 로직을 여기서 처리
 * 
 * 데이터 흐름:
 * 1. JwtApiUtil.createBithumbJwt() → JWT 토큰 생성
 * 2. BithumbFeignClient.getBalance() → 빗썸 API 호출
 * 3. BithumbConverter.toBalanceDTO() → 응답 변환
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BithumbAdapter implements ExchangeApiClient {

    private final BithumbFeignClient bithumbFeignClient;
    private final JwtApiUtil jwtApiUtil;

    @Override
    public BalanceResDTO.BalanceDTO getBalance(String phoneNumber, ExchangeType exchangeType) {
        try {
            // JWT 토큰 생성
            String jwt = jwtApiUtil.createBithumbJwt(phoneNumber, null, null);
            
            // 빗썸 API
            var responses = bithumbFeignClient.getBalance(jwt);
        
            return BithumbConverter.toBalanceDTO(responses);
            
        } catch (GeneralSecurityException e) {
            log.error("빗썸 JWT 생성 실패 - phoneNumber: {}, error: {}", phoneNumber, e.getMessage(), e);
            throw new RuntimeException("빗썸 인증 실패", e);
        } catch (Exception e) {
            log.error("빗썸 API 호출 실패 - phoneNumber: {}, error: {}", phoneNumber, e.getMessage(), e);
            throw new RuntimeException("빗썸 API 호출 실패", e);
        }
    }
}