package com.example.scoi.domain.invest.client.feign;

import com.example.scoi.global.client.dto.UpbitReqDTO;
import com.example.scoi.global.client.dto.UpbitResDTO;
import com.example.scoi.global.config.feign.UpbitFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
    name = "upbitFeignClient",
    url = "https://api.upbit.com",
    configuration = UpbitFeignConfig.class
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

    //주문 생성 테스트 (실제 주문 생성 없이 검증)
    @PostMapping("/v1/orders/test")
    UpbitResDTO.CreateOrder testCreateOrder(
            @RequestHeader("Authorization") String authorization,
            @RequestBody UpbitReqDTO.CreateOrder request
    );

    //주문 생성
    @PostMapping("/v1/orders")
    UpbitResDTO.CreateOrder createOrder(
            @RequestHeader("Authorization") String authorization,
            @RequestBody UpbitReqDTO.CreateOrder request
    );

    //주문 취소
    @DeleteMapping("/v1/order")
    UpbitResDTO.CancelOrder cancelOrder(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("uuid") String uuid
    );

    //현재가 조회 (PUBLIC API - 인증 불필요)
    @GetMapping("/v1/ticker")
    List<UpbitResDTO.Ticker> getTicker(@RequestParam("markets") String markets);
}