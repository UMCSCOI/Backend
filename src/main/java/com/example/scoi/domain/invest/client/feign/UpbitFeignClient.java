package com.example.scoi.domain.invest.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "upbitFeignClient",
    url = "https://api.upbit.com",
    configuration = com.example.scoi.global.config.feign.UpbitFeignConfig.class
)
public interface UpbitFeignClient {
    //전체계좌조회회
    @GetMapping("/v1/accounts")
    String getAccounts(@RequestHeader("Authorization") String authorization);

    //주문 가능 정보 조회
    
    @GetMapping("/v1/orders/chance")
    String getOrderChance(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("market") String market
    );
}