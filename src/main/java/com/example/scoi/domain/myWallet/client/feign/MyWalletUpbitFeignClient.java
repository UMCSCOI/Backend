package com.example.scoi.domain.myWallet.client.feign;

import com.example.scoi.domain.myWallet.dto.BalanceClientDTO;
import com.example.scoi.domain.myWallet.dto.TopupClientDTO;
import com.example.scoi.global.client.dto.UpbitResDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
    name = "myWalletUpbitFeignClient",
    url = "https://api.upbit.com",
    configuration = com.example.scoi.global.config.feign.UpbitFeignConfig.class
)
public interface MyWalletUpbitFeignClient {

    // 입금 목록 조회
    @GetMapping("/v1/deposits")
    List<UpbitResDTO.GetDeposit> getDeposits(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "currency", required = false) String currency,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "order_by", required = false) String orderBy
    );

    // 출금 목록 조회
    @GetMapping("/v1/withdraws")
    List<UpbitResDTO.GetWithdraw> getWithdraws(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "currency", required = false) String currency,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "order_by", required = false) String orderBy
    );

    // 전체 계좌 조회 (잔고)
    @GetMapping("/v1/accounts")
    List<BalanceClientDTO.AccountInfo> getAccounts(
            @RequestHeader("Authorization") String authorization
    );

    // 종료 주문 목록 조회 (완료/취소)
    @GetMapping("/v1/orders/closed")
    List<TopupClientDTO.UpbitOrder> getClosedOrders(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "market", required = false) String market,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "order_by", required = false) String orderBy,
            @RequestParam(value = "start_time", required = false) String startTime,
            @RequestParam(value = "end_time", required = false) String endTime
    );

    // 체결 대기 주문 목록 조회 (대기)
    @GetMapping("/v1/orders/open")
    List<TopupClientDTO.UpbitOrder> getOpenOrders(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "market", required = false) String market,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "order_by", required = false) String orderBy
    );

    // 개별 입금 조회
    @GetMapping("/v1/deposit")
    UpbitResDTO.GetDeposit getDeposit(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "uuid", required = false) String uuid,
            @RequestParam(value = "txid", required = false) String txid,
            @RequestParam(value = "currency", required = false) String currency
    );

    // 개별 출금 조회
    @GetMapping("/v1/withdraw")
    UpbitResDTO.GetWithdraw getWithdraw(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "uuid", required = false) String uuid,
            @RequestParam(value = "txid", required = false) String txid,
            @RequestParam(value = "currency", required = false) String currency
    );

    // 개별 주문 조회
    @GetMapping("/v1/order")
    UpbitResDTO.GetOrder getOrder(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "uuid") String uuid
    );
}
