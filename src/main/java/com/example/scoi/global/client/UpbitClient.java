package com.example.scoi.global.client;

import com.example.scoi.domain.member.dto.MemberReqDTO;
import com.example.scoi.domain.transfer.dto.TransferReqDTO;
import com.example.scoi.global.client.dto.BithumbResDTO;
import com.example.scoi.global.client.dto.UpbitReqDTO;
import com.example.scoi.global.client.dto.UpbitResDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "upbitClient",
        url = "https://api.upbit.com"
)
public interface UpbitClient {

    // 주문 리스트 조회
    @GetMapping("/v1/orders/closed")
    String getOrders(@RequestHeader("Authorization") String token);

    // 원화 입금
    @PostMapping("/v1/deposits/krw")
    UpbitResDTO.ChargeKrw chargeKrw(
            @RequestHeader("Authorization") String token,
            @RequestBody UpbitReqDTO.ChargeKrw dto
    );

    // 개별 주문 조회
    @GetMapping("/v1/order")
    UpbitResDTO.GetOrder getOrder(
            @RequestHeader("Authorization") String token,
            @RequestParam("uuid") String uuid
    );

    // 개별 입금 조회
    @GetMapping("/v1/deposit")
    UpbitResDTO.GetDeposit getDeposit(
            @RequestHeader("Authorization") String token,
            @RequestParam("uuid") String uuid,
            @RequestParam("currency") String currency
    );

    // 전체 계좌 조회
    // 쿼리파라미터 X & Request Body X
    // @GetMapping("/v1/accounts")
    // String getAccount(@RequestHeader("Authorization") String authorization);
    
    // 전체 계좌 조회 (DTO 반환)
    @GetMapping("/v1/accounts")
    UpbitResDTO.BalanceResponse[] getAccount(@RequestHeader("Authorization") String authorization);

    //보유자산 조회
    // @GetMapping("/v1/accounts")
    // UpbitResDTO.BalanceResponse[] getBalance(@RequestHeader("Authorization") String authorization);

    // 페어별 주문 가능 정보 조회
    // 쿼리파라미터 O
    @GetMapping("/v1/orders/chance")
    String getOrderChance(
            @RequestHeader("Authorization") String token,
            @RequestParam("market") String market
    );

    // 입금 주소 생성 요청
    // Request Body O
    @PostMapping("/v1/deposits/generate_coin_address")
    String getDepositAddress(
            @RequestHeader("Authorization") String token,
            @RequestBody MemberReqDTO.Test dto
    );

    // 출금(이체) 가능 금액 조회
    // 쿼리파라미터 O
    @GetMapping("/v1/withdraws/chance")
    UpbitResDTO.WithdrawsChance getWithdrawsChance(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("currency") String currency,
            @RequestParam("net_type") String netType
    );

    // 이체
    // Request Body O
    @PostMapping("/v1/withdraws/coin")
    UpbitResDTO.WithdrawResDTO withdrawCoin(
            @RequestHeader("Authorization") String authorization,
            @RequestBody TransferReqDTO.UpbitWithdrawRequest dto
    );

    // 출금 허용 주소 목록 조회 (수취인 조회)
    @GetMapping("/v1/withdraws/coin_addresses")
    List<UpbitResDTO.WithdrawalAddressResponse> getRecipients(
            @RequestHeader("Authorization") String authorization
    );
}
