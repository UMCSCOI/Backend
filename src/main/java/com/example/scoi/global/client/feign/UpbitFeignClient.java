package com.example.scoi.global.client.feign;

import com.example.scoi.global.client.dto.UpbitResDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 업비트 API를 호출하는 Feign Client 인터페이스
 * 
 * 역할:
 * - 업비트 공식 API와의 HTTP 통신을 담당
 * - JWT 인증 헤더를 포함하여 API 호출
 * - 거래소별 특화된 요청/응답 형식 처리
 * 
 * 사용 방법:
 * - Adapter에서 이 인터페이스를 주입받아 사용
 * - JWT 토큰은 JwtApiUtil에서 생성하여 헤더에 포함
 */
@FeignClient(
        name = "upbitFeignClient",
        url = "https://api.upbit.com"
)
public interface UpbitFeignClient {
   
    // 계정 잔고 조회 API
   
    @GetMapping("/v1/accounts")
    UpbitResDTO.BalanceResponse[] getBalance(
            @RequestHeader("Authorization") String authorization
    );
}