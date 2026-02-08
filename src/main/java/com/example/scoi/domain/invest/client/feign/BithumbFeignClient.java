package com.example.scoi.domain.invest.client.feign;

import com.example.scoi.global.client.dto.BithumbReqDTO;
import com.example.scoi.global.client.dto.BithumbResDTO;
import com.example.scoi.global.config.feign.BithumbFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "bithumbFeignClient",
    url = "https://api.bithumb.com",
    configuration = BithumbFeignConfig.class
)
public interface BithumbFeignClient {

    //전체 계좌 조회
    @GetMapping("/v1/accounts")
    String getAccounts(@RequestHeader("Authorization") String authorization);

    //주문 가능 정보 조회
    @GetMapping("/v1/orders/chance")
    BithumbResDTO.OrderChance getOrderChance(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("market") String market
    );

    //주문 생성
    @PostMapping("/v1/orders")
    BithumbResDTO.CreateOrder createOrder(
            @RequestHeader("Authorization") String authorization,
            @RequestBody BithumbReqDTO.CreateOrder request
    );

    //주문 취소
    @DeleteMapping("/v1/order")
    BithumbResDTO.CancelOrder cancelOrder(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("uuid") String uuid
    );
    
    //마켓 코드 조회 (PUBLIC API - 인증 불필요)
    @GetMapping("/v1/market/all")
    String getMarketAll(@RequestParam(value = "isDetails", required = false) Boolean isDetails);
}