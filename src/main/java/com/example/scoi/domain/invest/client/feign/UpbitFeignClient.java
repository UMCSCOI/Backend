package com.example.scoi.domain.invest.client.feign;

import com.example.scoi.global.client.dto.UpbitResDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
    name = "upbitFeignClient",
    url = "https://api.upbit.com",
    configuration = com.example.scoi.global.config.feign.UpbitFeignConfig.class
)
public interface UpbitFeignClient {
    //전체계좌조회
    @GetMapping("/v1/accounts")
    List<UpbitResDTO.Account> getAccounts(@RequestHeader("Authorization") String authorization);

    //주문 가능 정보 조회
    @GetMapping("/v1/orders/chance")
    UpbitResDTO.OrderChance getOrderChance(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("market") String market
    );
}