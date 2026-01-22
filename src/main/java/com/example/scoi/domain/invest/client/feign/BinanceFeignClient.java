package com.example.scoi.domain.invest.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "binanceFeignClient",
    url = "https://api.binance.com",
    configuration = com.example.scoi.global.config.feign.BinanceFeignConfig.class
)
public interface BinanceFeignClient {

    //계좌 정보 조회
    @GetMapping("/api/v3/account")
    String getAccount(
            @RequestHeader("X-MBX-APIKEY") String apiKey,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("signature") String signature
    );

  //주문 가능 정보 조회
    @GetMapping("/api/v3/order")
    String getOrderInfo(
            @RequestHeader("X-MBX-APIKEY") String apiKey,
            @RequestParam("symbol") String symbol,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("signature") String signature
    );
}