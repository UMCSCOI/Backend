package com.example.scoi.domain.myWallet.client.adapter;

import com.example.scoi.domain.myWallet.client.MyWalletExchangeClient;
import com.example.scoi.domain.myWallet.client.feign.MyWalletBithumbFeignClient;
import com.example.scoi.domain.myWallet.converter.MyWalletConverter;
import com.example.scoi.domain.myWallet.dto.MyWalletResDTO;
import com.example.scoi.domain.myWallet.dto.TopupClientDTO;
import com.example.scoi.domain.myWallet.dto.WithdrawClientDTO;
import com.example.scoi.domain.myWallet.enums.OrderState;
import com.example.scoi.domain.myWallet.enums.PeriodType;
import com.example.scoi.domain.member.exception.MemberException;
import com.example.scoi.domain.myWallet.exception.MyWalletException;
import com.example.scoi.domain.myWallet.exception.code.MyWalletErrorCode;
import com.example.scoi.global.client.dto.BithumbResDTO;
import com.example.scoi.global.util.JwtApiUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MyWalletBithumbClient implements MyWalletExchangeClient {

    private final MyWalletBithumbFeignClient bithumbFeignClient;
    private final JwtApiUtil jwtApiUtil;

    private static final int MAX_LIMIT = 100;
    private static final String ORDER_DESC = "desc";
    private static final List<String> CURRENCIES = List.of("USDT", "USDC");
    private static final List<String> MARKETS = List.of("KRW-USDT", "KRW-USDC");

    @Override
    public Map<String, BigDecimal> getBalances(String phoneNumber) {
        try {
            log.info("빗썸 계좌 잔고 조회 시작 - phoneNumber: {}", phoneNumber);

            String authorization = jwtApiUtil.createBithumbJwt(phoneNumber, "", null);
            var accounts = bithumbFeignClient.getAccounts(authorization);

            Map<String, BigDecimal> balances = new HashMap<>();
            for (var account : accounts) {
                if (CURRENCIES.contains(account.currency())) {
                    BigDecimal total = new BigDecimal(account.balance())
                            .add(new BigDecimal(account.locked()));
                    balances.put(account.currency(), total);
                }
            }

            for (String currency : CURRENCIES) {
                balances.putIfAbsent(currency, BigDecimal.ZERO);
            }

            log.info("빗썸 계좌 잔고 조회 완료 - balances: {}", balances);
            return balances;

        } catch (Exception e) {
            throw handleException("빗썸 계좌 잔고 조회", phoneNumber, e);
        }
    }

    @Override
    public List<MyWalletResDTO.TransactionDTO> getDeposits(String phoneNumber) {
        try {
            log.info("빗썸 코인 입금 리스트 조회 시작 - phoneNumber: {}", phoneNumber);

            List<MyWalletResDTO.TransactionDTO> allDeposits = new ArrayList<>();

            for (String currency : CURRENCIES) {
                String query = "currency=" + currency + "&limit=" + MAX_LIMIT + "&page=1&order_by=" + ORDER_DESC;
                String authorization = jwtApiUtil.createBithumbJwt(phoneNumber, query, null);
                List<BithumbResDTO.GetDeposit> deposits = bithumbFeignClient.getDeposits(authorization, currency, MAX_LIMIT, 1, ORDER_DESC);

                log.info("빗썸 {} 입금 조회 완료 - 건수: {}", currency, deposits.size());
                allDeposits.addAll(deposits.stream()
                        .map(MyWalletConverter::fromBithumbDeposit)
                        .toList());
            }

            log.info("빗썸 코인 입금 리스트 조회 완료 - 총 건수: {}", allDeposits.size());
            return allDeposits;

        } catch (Exception e) {
            throw handleException("빗썸 코인 입금 리스트 조회", phoneNumber, e);
        }
    }

    @Override
    public List<MyWalletResDTO.TransactionDTO> getWithdraws(String phoneNumber) {
        try {
            log.info("빗썸 코인 출금 리스트 조회 시작 - phoneNumber: {}", phoneNumber);

            List<MyWalletResDTO.TransactionDTO> allWithdraws = new ArrayList<>();

            for (String currency : CURRENCIES) {
                String query = "currency=" + currency + "&limit=" + MAX_LIMIT + "&page=1&order_by=" + ORDER_DESC;
                String authorization = jwtApiUtil.createBithumbJwt(phoneNumber, query, null);
                List<BithumbResDTO.GetWithdraw> withdraws = bithumbFeignClient.getWithdraws(authorization, currency, MAX_LIMIT, 1, ORDER_DESC);

                log.info("빗썸 {} 출금 조회 완료 - 건수: {}", currency, withdraws.size());
                allWithdraws.addAll(withdraws.stream()
                        .map(MyWalletConverter::fromBithumbWithdraw)
                        .toList());
            }

            log.info("빗썸 코인 출금 리스트 조회 완료 - 총 건수: {}", allWithdraws.size());
            return allWithdraws;

        } catch (Exception e) {
            throw handleException("빗썸 코인 출금 리스트 조회", phoneNumber, e);
        }
    }

    @Override
    public List<MyWalletResDTO.TopupTransactionDTO> getOrders(
            String phoneNumber, OrderState state, PeriodType periodType, String order, int limit) {
        try {
            log.info("빗썸 주문 리스트 조회 시작 - phoneNumber: {}, state: {}, order: {}", phoneNumber, state, order);

            List<MyWalletResDTO.TopupTransactionDTO> allOrders = new ArrayList<>();
            String stateValue = state.toApiValue();

            for (String market : MARKETS) {
                String query = "market=" + market + "&state=" + stateValue
                        + "&limit=" + MAX_LIMIT + "&page=1&order_by=" + order;
                String authorization = jwtApiUtil.createBithumbJwt(phoneNumber, query, null);
                List<TopupClientDTO.BithumbOrder> orders = bithumbFeignClient.getOrders(
                        authorization, market, stateValue, MAX_LIMIT, 1, order);

                log.info("빗썸 {} 주문 조회 완료 - state: {}, 건수: {}", market, stateValue, orders.size());
                allOrders.addAll(orders.stream()
                        .map(MyWalletConverter::fromBithumbOrder)
                        .toList());
            }

            log.info("빗썸 주문 리스트 조회 완료 - 총 건수: {}", allOrders.size());
            return allOrders;

        } catch (Exception e) {
            throw handleException("빗썸 주문 리스트 조회", phoneNumber, e);
        }
    }

    @Override
    public MyWalletResDTO.RemitDetailDTO getDepositDetail(String phoneNumber, String uuid, String currency) {
        try {
            log.info("빗썸 개별 입금 조회 시작 - uuid: {}, currency: {}", uuid, currency);

            String query = buildDetailQuery(uuid, currency);
            String authorization = jwtApiUtil.createBithumbJwt(phoneNumber, query, null);
            BithumbResDTO.GetDeposit deposit = bithumbFeignClient.getDeposit(authorization, uuid, null, currency);

            log.info("빗썸 개별 입금 조회 완료 - uuid: {}", uuid);
            return MyWalletConverter.fromBithumbDepositDetail(deposit);

        } catch (Exception e) {
            throw handleException("빗썸 개별 입금 조회", phoneNumber, e);
        }
    }

    @Override
    public MyWalletResDTO.RemitDetailDTO getWithdrawDetail(String phoneNumber, String uuid, String currency) {
        try {
            log.info("빗썸 개별 출금 조회 시작 - uuid: {}, currency: {}", uuid, currency);

            String query = buildDetailQuery(uuid, currency);
            String authorization = jwtApiUtil.createBithumbJwt(phoneNumber, query, null);
            BithumbResDTO.GetWithdraw withdraw = bithumbFeignClient.getWithdraw(authorization, uuid, null, currency);

            log.info("빗썸 개별 출금 조회 완료 - uuid: {}", uuid);
            return MyWalletConverter.fromBithumbWithdrawDetail(withdraw);

        } catch (Exception e) {
            throw handleException("빗썸 개별 출금 조회", phoneNumber, e);
        }
    }

    /**
     * 개별 입출금 조회용 query string 생성 (uuid + currency)
     */
    private String buildDetailQuery(String uuid, String currency) {
        StringBuilder query = new StringBuilder("uuid=" + uuid);
        if (currency != null && !currency.isBlank()) {
            query.append("&currency=").append(currency);
        }
        return query.toString();
    }

    @Override
    public MyWalletResDTO.TopupDetailDTO getOrderDetail(String phoneNumber, String uuid) {
        try {
            log.info("빗썸 개별 주문 조회 시작 - uuid: {}", uuid);

            String query = "uuid=" + uuid;
            String authorization = jwtApiUtil.createBithumbJwt(phoneNumber, query, null);
            BithumbResDTO.GetOrder order = bithumbFeignClient.getOrder(authorization, uuid);

            log.info("빗썸 개별 주문 조회 완료 - uuid: {}", uuid);
            return MyWalletConverter.fromBithumbOrderDetail(order);

        } catch (Exception e) {
            throw handleException("빗썸 개별 주문 조회", phoneNumber, e);
        }
    }

    @Override
    public MyWalletResDTO.KrwBalanceDTO getKrwBalance(String phoneNumber) {
        try {
            log.info("빗썸 원화 자산 조회 시작 - phoneNumber: {}", phoneNumber);

            String authorization = jwtApiUtil.createBithumbJwt(phoneNumber, "", null);
            var accounts = bithumbFeignClient.getAccounts(authorization);

            for (var account : accounts) {
                if ("KRW".equals(account.currency())) {
                    log.info("빗썸 원화 자산 조회 완료 - balance: {}", account.balance());
                    return MyWalletResDTO.KrwBalanceDTO.builder()
                            .currency("KRW")
                            .balance(account.balance() != null ? account.balance() : "0")
                            .build();
                }
            }

            // KRW 계좌가 없는 경우 0으로 반환
            log.info("빗썸 원화 계좌 없음 - 0 반환");
            return MyWalletResDTO.KrwBalanceDTO.builder()
                    .currency("KRW")
                    .balance("0")
                    .build();

        } catch (Exception e) {
            throw handleException("빗썸 원화 자산 조회", phoneNumber, e);
        }
    }

    @Override
    public MyWalletResDTO.WithdrawKrwDTO withdrawKrw(String phoneNumber, Long amount, String mfaType) {
        try {
            log.info("빗썸 원화 출금 요청 시작 - phoneNumber: {}, amount: {}", phoneNumber, amount);

            WithdrawClientDTO.WithdrawKrwRequest requestBody = WithdrawClientDTO.WithdrawKrwRequest.builder()
                    .amount(amount.toString())
                    .two_factor_type(mfaType)
                    .build();

            String authorization = jwtApiUtil.createBithumbJwt(phoneNumber, null, requestBody);
            WithdrawClientDTO.WithdrawKrwResponse response = bithumbFeignClient.withdrawKrw(authorization, requestBody);

            log.info("빗썸 원화 출금 요청 완료 - uuid: {}", response.uuid());
            return MyWalletResDTO.WithdrawKrwDTO.builder()
                    .currency("KRW")
                    .uuid(response.uuid())
                    .txid(response.txid())
                    .build();

        } catch (Exception e) {
            throw handleException("빗썸 원화 출금 요청", phoneNumber, e);
        }
    }

    /**
     * 공통 예외 처리: Feign 예외를 세분화하여 적절한 MyWalletException으로 변환
     */
    private MyWalletException handleException(String operation, String phoneNumber, Exception e) {
        // 이미 MyWalletException이면 그대로 전달
        if (e instanceof MyWalletException mwe) {
            return mwe;
        }
        // API 키 미등록
        if (e instanceof MemberException) {
            log.error("{} 실패 - API 키 미등록 - phoneNumber: {}", operation, phoneNumber);
            return new MyWalletException(MyWalletErrorCode.API_KEY_NOT_FOUND);
        }
        // JWT 생성 실패
        if (e instanceof GeneralSecurityException) {
            log.error("{} 실패 - JWT 생성 실패", operation, e);
            return new MyWalletException(MyWalletErrorCode.EXCHANGE_API_ERROR);
        }
        // Feign 401: 권한 부족
        if (e instanceof feign.FeignException.Unauthorized) {
            log.error("{} 실패 - API 키 권한 부족", operation, e);
            return new MyWalletException(MyWalletErrorCode.INSUFFICIENT_API_PERMISSION);
        }
        // Feign 429: Rate Limit 초과
        if (e instanceof feign.FeignException.TooManyRequests) {
            log.error("{} 실패 - 거래소 API 호출 한도 초과", operation, e);
            return new MyWalletException(MyWalletErrorCode.EXCHANGE_RATE_LIMIT);
        }
        // Feign 500+: 거래소 서버 에러
        if (e instanceof feign.FeignException.InternalServerError
                || e instanceof feign.FeignException.BadGateway
                || e instanceof feign.FeignException.ServiceUnavailable) {
            log.error("{} 실패 - 거래소 서버 에러", operation, e);
            return new MyWalletException(MyWalletErrorCode.EXCHANGE_SERVER_ERROR);
        }
        // Feign 504 또는 타임아웃
        if (e instanceof feign.FeignException.GatewayTimeout
                || e instanceof feign.RetryableException) {
            log.error("{} 실패 - 거래소 API 타임아웃", operation, e);
            return new MyWalletException(MyWalletErrorCode.EXCHANGE_TIMEOUT);
        }
        // 그 외 에러
        log.error("{} 실패", operation, e);
        return new MyWalletException(MyWalletErrorCode.EXCHANGE_API_ERROR);
    }
}
