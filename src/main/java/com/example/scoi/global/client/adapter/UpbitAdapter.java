package com.example.scoi.global.client.adapter;

import com.example.scoi.domain.charge.dto.BalanceResDTO;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.global.client.converter.UpbitConverter;
import com.example.scoi.global.client.feign.UpbitFeignClient;
import com.example.scoi.global.util.JwtApiUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.GeneralSecurityException;


@Component
@RequiredArgsConstructor
@Slf4j
public class UpbitAdapter implements ExchangeApiClient {

    private final UpbitFeignClient upbitFeignClient;
    private final JwtApiUtil jwtApiUtil;

    @Override
    public BalanceResDTO.BalanceDTO getBalance(String phoneNumber, ExchangeType exchangeType) {
        try {
      
            String jwt = jwtApiUtil.createUpBitJwt(phoneNumber, null, null);
            
     
            var responses = upbitFeignClient.getBalance(jwt);
            

            return UpbitConverter.toBalanceDTO(responses);
            
        } catch (GeneralSecurityException e) {
            log.error("업비트 JWT 생성 실패 - phoneNumber: {}, error: {}", phoneNumber, e.getMessage(), e);
            throw new RuntimeException("업비트 인증 실패", e);
        } catch (Exception e) {
            log.error("업비트 API 호출 실패 - phoneNumber: {}, error: {}", phoneNumber, e.getMessage(), e);
            throw new RuntimeException("업비트 API 호출 실패", e);
        }
    }
}