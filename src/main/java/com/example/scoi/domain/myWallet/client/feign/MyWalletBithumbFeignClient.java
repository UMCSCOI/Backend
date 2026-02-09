package com.example.scoi.domain.myWallet.client.feign;

import com.example.scoi.domain.myWallet.dto.BalanceClientDTO;
import com.example.scoi.domain.myWallet.dto.TopupClientDTO;
import com.example.scoi.domain.myWallet.dto.WithdrawClientDTO;
import com.example.scoi.global.client.dto.BithumbResDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
    name = "myWalletBithumbFeignClient",
    url = "https://api.bithumb.com",
    configuration = com.example.scoi.global.config.feign.BithumbFeignConfig.class
)
public interface MyWalletBithumbFeignClient {

    // 코인 입금 리스트 조회
    @GetMapping("/v1/deposits")
    List<BithumbResDTO.GetDeposit> getDeposits(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "currency", required = false) String currency,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "order_by", required = false) String orderBy
    );

    // 코인 출금 리스트 조회
    @GetMapping("/v1/withdraws")
    List<BithumbResDTO.GetWithdraw> getWithdraws(
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

    // 주문 리스트 조회 (충전 거래 내역)
    @GetMapping("/v1/orders")
    List<TopupClientDTO.BithumbOrder> getOrders(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "market", required = false) String market,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "order_by", required = false) String orderBy
    );

    // 개별 입금 조회
    @GetMapping("/v1/deposit")
    BithumbResDTO.GetDeposit getDeposit(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "uuid", required = false) String uuid,
            @RequestParam(value = "txid", required = false) String txid,
            @RequestParam(value = "currency", required = false) String currency
    );

    // 개별 출금 조회
    @GetMapping("/v1/withdraw")
    BithumbResDTO.GetWithdraw getWithdraw(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "uuid", required = false) String uuid,
            @RequestParam(value = "txid", required = false) String txid,
            @RequestParam(value = "currency", required = false) String currency
    );

    // 개별 주문 조회
    @GetMapping("/v1/order")
    BithumbResDTO.GetOrder getOrder(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "uuid") String uuid
    );

    // 원화 출금 요청
    @PostMapping("/v1/withdraws/krw")
    WithdrawClientDTO.WithdrawKrwResponse withdrawKrw(
            @RequestHeader("Authorization") String authorization,
            @RequestBody WithdrawClientDTO.WithdrawKrwRequest dto
    );
}
