package com.example.scoi.global.client;

import com.example.scoi.domain.member.dto.MemberReqDTO;
import com.example.scoi.global.client.dto.BithumbReqDTO;
import com.example.scoi.global.client.dto.BithumbResDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "bithumbClient",
        url = "https://api.bithumb.com"
)
public interface BithumbClient {

    // 원화 입금
    @PostMapping("/v1/deposits/krw")
    BithumbResDTO.ChargeKrw chargeKrw(
            @RequestHeader("Authorization") String token,
            @RequestBody BithumbReqDTO.ChargeKrw dto
    );

    // 개별 주문 조회
    @GetMapping("/v1/order")
    BithumbResDTO.GetOrder getOrder(
            @RequestHeader("Authorization") String token,
            @RequestParam("uuid") String uuid
    );

    // 개별 입금 조회
    @GetMapping("/v1/deposit")
    BithumbResDTO.GetDeposit getDeposit(
            @RequestHeader("Authorization") String token,
            @RequestParam("uuid") String uuid,
            @RequestParam("currency") String currency
    );

    // 전체 계좌 조회
    // 쿼리파라미터 & Request Body X
    @GetMapping("/v1/accounts")
    String getAccount(@RequestHeader("Authorization") String authorization);

    // 주문 가능 정보
    // 쿼리파라미터 O
    @GetMapping("/v1/orders/chance")
    String getOrderChance(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("market") String market
    );

    // 입금 주소 생성 요청
    // Request Body O
    @PostMapping("/v1/deposits/generate_coin_address")
    String getDepositAddress(
            @RequestHeader("Authorization") String authorization,
            @RequestBody MemberReqDTO.Test dto
    );
}
