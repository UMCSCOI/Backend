package com.example.scoi.domain.member.client;

import com.example.scoi.domain.member.dto.MemberReqDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "bithumb-client",
        url = "https://api.bithumb.com"
)
public interface BithumbClient {

    // 쿼리파라미터 & Request Body X
    @GetMapping("/v1/accounts")
    String getAccount(@RequestHeader("Authorization") String authorization);

    // 쿼리파라미터 O
    @GetMapping("/v1/orders/chance")
    String getOrderChance(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("market") String market
    );

    // Request Body O
    @PostMapping("/v1/deposits/generate_coin_address")
    String getDepositAddress(
            @RequestHeader("Authorization") String authorization,
            @RequestBody MemberReqDTO.Test dto
    );
}
