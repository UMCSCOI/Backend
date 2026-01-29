package com.example.scoi.global.client.feign;

import com.example.scoi.global.client.dto.BithumbResDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 빗썸 API를 호출하는 Feign Client 인터페이스
 * - Adapter에서 이 인터페이스를 주입받아 사용
 * - JWT 토큰은 JwtApiUtil에서 생성하여 헤더에 포함
 */
@FeignClient(
        name = "bithumbFeignClient",
        url = "https://api.bithumb.com"
)
public interface BithumbFeignClient {

   //전체 계좌 조회 API (보유 자산 조회)
    @GetMapping("/v1/accounts")
    BithumbResDTO.BalanceResponse[] getBalance(
            @RequestHeader("Authorization") String authorization
    );
}