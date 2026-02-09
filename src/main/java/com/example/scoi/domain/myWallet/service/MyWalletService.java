package com.example.scoi.domain.myWallet.service;

import com.example.scoi.domain.member.entity.Member;
import com.example.scoi.domain.member.enums.ExchangeType;
import com.example.scoi.domain.member.repository.MemberRepository;
import com.example.scoi.domain.myWallet.client.MyWalletExchangeClient;
import com.example.scoi.domain.myWallet.client.adapter.MyWalletBithumbClient;
import com.example.scoi.domain.myWallet.client.adapter.MyWalletUpbitClient;
import com.example.scoi.domain.myWallet.converter.MyWalletConverter;
import com.example.scoi.domain.myWallet.dto.MyWalletResDTO;
import com.example.scoi.domain.myWallet.enums.DetailCategory;
import com.example.scoi.domain.myWallet.enums.OrderState;
import com.example.scoi.domain.myWallet.enums.PeriodType;
import com.example.scoi.domain.myWallet.enums.RemitType;
import com.example.scoi.domain.myWallet.enums.TopupType;
import com.example.scoi.domain.myWallet.exception.MyWalletException;
import com.example.scoi.domain.myWallet.exception.code.MyWalletErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MyWalletService {

    private final MemberRepository memberRepository;
    private final MyWalletBithumbClient myWalletBithumbClient;
    private final MyWalletUpbitClient myWalletUpbitClient;

    /**
     * 거래 내역(입출금) 전체 조회
     *
     * @param phoneNumber  사용자 휴대폰 번호
     * @param exchangeType 거래소 타입 (BITHUMB, UPBIT)
     * @param remitType    조회 유형 (ALL, DEPOSIT, WITHDRAW)
     * @param periodType   기간 (TODAY, ONE_MONTH, THREE_MONTHS, SIX_MONTHS)
     * @param order        정렬 방향 (desc, asc)
     * @param limit        조회 건수
     * @return 거래 내역 목록 (각 항목에 해당 통화의 잔량 포함)
     */
    public MyWalletResDTO.TransactionListDTO getRemitTransactions(
            String phoneNumber,
            ExchangeType exchangeType,
            RemitType remitType,
            PeriodType periodType,
            String order,
            int limit
    ) {
        // 1. 사용자 존재 여부 확인
        memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new MyWalletException(MyWalletErrorCode.MEMBER_NOT_FOUND));

        // 2. 거래소 클라이언트 선택
        MyWalletExchangeClient apiClient = getApiClient(exchangeType);

        // 3. 현재 잔고 조회 + 입금/출금 데이터 모두 조회 (잔량 계산을 위해 항상 양쪽 다 가져옴)
        Map<String, BigDecimal> currentBalances;
        List<MyWalletResDTO.TransactionDTO> allTransactions = new ArrayList<>();

        try {
            currentBalances = apiClient.getBalances(phoneNumber);

            List<MyWalletResDTO.TransactionDTO> deposits = apiClient.getDeposits(phoneNumber);
            allTransactions.addAll(deposits);

            List<MyWalletResDTO.TransactionDTO> withdraws = apiClient.getWithdraws(phoneNumber);
            allTransactions.addAll(withdraws);
        } catch (MyWalletException e) {
            throw e;
        } catch (Exception e) {
            log.error("거래소 API 호출 실패 - exchangeType: {}, phoneNumber: {}", exchangeType, phoneNumber, e);
            throw new MyWalletException(MyWalletErrorCode.EXCHANGE_API_ERROR);
        }

        // 4. 최신순 정렬 (잔량 역산을 위해 항상 최신순으로 먼저 정렬)
        List<MyWalletResDTO.TransactionDTO> sortedDesc = allTransactions.stream()
                .sorted(Comparator.comparing(
                        MyWalletResDTO.TransactionDTO::createdAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .toList();

        // 5. 잔량 역산 계산 (최신순으로 순회하며 통화별 잔량 계산)
        //    잔량에 영향을 주는 상태:
        //    - 입금(DEPOSIT): ACCEPTED (입금 완료)
        //    - 출금(WITHDRAW): DONE (출금 완료)
        //    그 외 상태(PROCESSING, WAITING, CANCELLED, REJECTED, FAILED, REFUNDED 등)는 잔량 변화 없음
        Map<String, BigDecimal> runningBalances = new HashMap<>(currentBalances);
        List<MyWalletResDTO.TransactionDTO> withBalances = new ArrayList<>();

        for (MyWalletResDTO.TransactionDTO tx : sortedDesc) {
            String currency = tx.currency();
            BigDecimal balance = runningBalances.getOrDefault(currency, BigDecimal.ZERO);

            // 현재 거래의 잔량 = 이 거래 직후의 잔량
            withBalances.add(MyWalletConverter.withBalance(tx, balance.toPlainString()));

            // 완료된 거래만 잔량 역산에 반영
            if (isBalanceAffectingState(tx.type(), tx.state())) {
                BigDecimal amount = parseAmount(tx.amount());
                if (tx.type() == RemitType.DEPOSIT) {
                    // 입금이었으므로, 이전에는 이 금액만큼 적었음
                    runningBalances.put(currency, balance.subtract(amount));
                } else if (tx.type() == RemitType.WITHDRAW) {
                    // 출금이었으므로, 이전에는 이 금액만큼 많았음
                    runningBalances.put(currency, balance.add(amount));
                }
            }
        }

        // 6. 기간 필터링
        LocalDate startDate = periodType.getStartDate();
        List<MyWalletResDTO.TransactionDTO> filtered = withBalances.stream()
                .filter(tx -> isWithinPeriod(tx.createdAt(), startDate))
                .toList();

        // 7. remitType 필터링 (잔량 계산 후 필터링)
        if (remitType == RemitType.DEPOSIT) {
            filtered = filtered.stream()
                    .filter(tx -> tx.type() == RemitType.DEPOSIT)
                    .toList();
        } else if (remitType == RemitType.WITHDRAW) {
            filtered = filtered.stream()
                    .filter(tx -> tx.type() == RemitType.WITHDRAW)
                    .toList();
        }

        // 8. 최종 정렬 (사용자 요청 순서)
        List<MyWalletResDTO.TransactionDTO> sorted;
        if ("asc".equalsIgnoreCase(order)) {
            // 과거순으로 다시 정렬
            sorted = filtered.stream()
                    .sorted(Comparator.comparing(
                            MyWalletResDTO.TransactionDTO::createdAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ))
                    .toList();
        } else {
            // 이미 최신순이므로 그대로 사용
            sorted = filtered;
        }

        // 9. limit 적용
        List<MyWalletResDTO.TransactionDTO> result = sorted.stream()
                .limit(limit)
                .toList();

        log.info("거래 내역 조회 완료 - exchangeType: {}, remitType: {}, period: {}, 전체: {}, 필터링: {}, 반환: {}",
                exchangeType, remitType, periodType, allTransactions.size(), filtered.size(), result.size());

        return MyWalletResDTO.TransactionListDTO.builder()
                .transactions(result)
                .totalCount(result.size())
                .build();
    }

    /**
     * 충전 거래 내역(주문) 전체 조회
     *
     * @param phoneNumber  사용자 휴대폰 번호
     * @param exchangeType 거래소 타입 (BITHUMB, UPBIT)
     * @param topupType    조회 유형 (ALL, CHARGE, CASH_EXCHANGE)
     * @param state        주문 상태 (DONE, WAIT, CANCEL)
     * @param periodType   기간 (TODAY, ONE_MONTH, THREE_MONTHS, SIX_MONTHS)
     * @param order        정렬 방향 (desc, asc)
     * @param limit        조회 건수
     * @return 충전 거래 내역 목록
     */
    public MyWalletResDTO.TopupTransactionListDTO getTopupTransactions(
            String phoneNumber,
            ExchangeType exchangeType,
            TopupType topupType,
            OrderState state,
            PeriodType periodType,
            String order,
            int limit
    ) {
        // 1. 사용자 존재 여부 확인
        memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new MyWalletException(MyWalletErrorCode.MEMBER_NOT_FOUND));

        // 2. 거래소 클라이언트 선택
        MyWalletExchangeClient apiClient = getApiClient(exchangeType);

        // 3. 거래소에서 주문 데이터 조회
        List<MyWalletResDTO.TopupTransactionDTO> allOrders;
        try {
            if (topupType == TopupType.ALL) {
                // 전체: bid와 ask 모두 가져와서 합침 (거래소 API에 전체 필터가 없으므로 서버에서 처리)
                allOrders = apiClient.getOrders(phoneNumber, state, periodType, order, limit);
            } else {
                allOrders = apiClient.getOrders(phoneNumber, state, periodType, order, limit);
            }
        } catch (MyWalletException e) {
            throw e;
        } catch (Exception e) {
            log.error("거래소 주문 API 호출 실패 - exchangeType: {}, phoneNumber: {}", exchangeType, phoneNumber, e);
            throw new MyWalletException(MyWalletErrorCode.EXCHANGE_API_ERROR);
        }

        // 4. side 필터링 (충전/현금교환)
        List<MyWalletResDTO.TopupTransactionDTO> filtered = allOrders;
        if (topupType == TopupType.CHARGE) {
            filtered = allOrders.stream()
                    .filter(tx -> "bid".equals(tx.side()))
                    .toList();
        } else if (topupType == TopupType.CASH_EXCHANGE) {
            filtered = allOrders.stream()
                    .filter(tx -> "ask".equals(tx.side()))
                    .toList();
        }

        // 5. 기간 필터링 (빗썸 데이터는 서버에서 필터링 필요)
        LocalDate startDate = periodType.getStartDate();
        filtered = filtered.stream()
                .filter(tx -> isWithinPeriod(tx.createdAt(), startDate))
                .toList();

        // 6. 정렬 (created_at 기준)
        Comparator<MyWalletResDTO.TopupTransactionDTO> comparator =
                Comparator.comparing(MyWalletResDTO.TopupTransactionDTO::createdAt, Comparator.nullsLast(Comparator.naturalOrder()));

        if ("asc".equalsIgnoreCase(order)) {
            // 과거순
        } else {
            // 최신순 (기본값)
            comparator = comparator.reversed();
        }

        List<MyWalletResDTO.TopupTransactionDTO> sorted = filtered.stream()
                .sorted(comparator)
                .toList();

        // 7. limit 적용
        List<MyWalletResDTO.TopupTransactionDTO> result = sorted.stream()
                .limit(limit)
                .toList();

        log.info("충전 거래 내역 조회 완료 - exchangeType: {}, topupType: {}, state: {}, period: {}, 전체: {}, 필터링: {}, 반환: {}",
                exchangeType, topupType, state, periodType, allOrders.size(), filtered.size(), result.size());

        return MyWalletResDTO.TopupTransactionListDTO.builder()
                .transactions(result)
                .totalCount(result.size())
                .build();
    }

    /**
     * 거래내역 상세 조회 (입출금 + 충전 통합)
     *
     * @param phoneNumber    사용자 휴대폰 번호
     * @param exchangeType   거래소 타입 (BITHUMB, UPBIT)
     * @param category       조회 카테고리 (REMIT: 입출금, TOPUP: 충전)
     * @param remitType      입출금 구분 (DEPOSIT, WITHDRAW) - category=REMIT일 때 필수
     * @param uuid           거래 UUID
     * @param currency       통화 코드 (USDT, USDC 등, nullable) - category=REMIT일 때 선택
     * @return 거래 상세 정보
     */
    public MyWalletResDTO.TransactionDetailDTO getTransactionDetail(
            String phoneNumber,
            ExchangeType exchangeType,
            DetailCategory category,
            RemitType remitType,
            String uuid,
            String currency
    ) {
        // 1. 사용자 존재 여부 확인
        memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new MyWalletException(MyWalletErrorCode.MEMBER_NOT_FOUND));

        // 2. 거래소 클라이언트 선택
        MyWalletExchangeClient apiClient = getApiClient(exchangeType);

        // 3. 카테고리별 상세 조회
        try {
            if (category == DetailCategory.REMIT) {
                MyWalletResDTO.RemitDetailDTO remitDetail;
                if (remitType == RemitType.DEPOSIT) {
                    remitDetail = apiClient.getDepositDetail(phoneNumber, uuid, currency);
                } else {
                    remitDetail = apiClient.getWithdrawDetail(phoneNumber, uuid, currency);
                }

                log.info("입출금 상세 조회 완료 - exchangeType: {}, remitType: {}, uuid: {}",
                        exchangeType, remitType, uuid);

                return MyWalletResDTO.TransactionDetailDTO.builder()
                        .category(category.name())
                        .remitDetail(remitDetail)
                        .build();

            } else {
                MyWalletResDTO.TopupDetailDTO topupDetail = apiClient.getOrderDetail(phoneNumber, uuid);

                log.info("충전 상세 조회 완료 - exchangeType: {}, uuid: {}", exchangeType, uuid);

                return MyWalletResDTO.TransactionDetailDTO.builder()
                        .category(category.name())
                        .topupDetail(topupDetail)
                        .build();
            }
        } catch (MyWalletException e) {
            throw e;
        } catch (Exception e) {
            log.error("거래 상세 조회 실패 - exchangeType: {}, category: {}, uuid: {}",
                    exchangeType, category, uuid, e);
            throw new MyWalletException(MyWalletErrorCode.EXCHANGE_API_ERROR);
        }
    }

    /**
     * 거래소 타입에 따라 적절한 API 클라이언트를 반환합니다.
     */
    private MyWalletExchangeClient getApiClient(ExchangeType exchangeType) {
        return switch (exchangeType) {
            case BITHUMB -> myWalletBithumbClient;
            case UPBIT -> myWalletUpbitClient;
        };
    }

    /**
     * 거래가 실제로 잔량에 영향을 주는 완료 상태인지 확인합니다.
     * - 입금(DEPOSIT): ACCEPTED일 때만 잔량 변화
     * - 출금(WITHDRAW): DONE일 때만 잔량 변화
     * - 그 외 상태(PROCESSING, WAITING, CANCELLED, REJECTED, FAILED, REFUNDED 등)는 잔량 변화 없음
     */
    private boolean isBalanceAffectingState(RemitType type, String state) {
        if (state == null) {
            return false;
        }
        String upperState = state.toUpperCase();
        if (type == RemitType.DEPOSIT) {
            return "ACCEPTED".equals(upperState);
        } else if (type == RemitType.WITHDRAW) {
            return "DONE".equals(upperState);
        }
        return false;
    }

    /**
     * 금액 문자열을 BigDecimal로 파싱합니다.
     * null이나 빈 값은 0으로 처리합니다.
     */
    private BigDecimal parseAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(amount);
        } catch (NumberFormatException e) {
            log.warn("금액 파싱 실패 - amount: {}", amount);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 거래 시간이 기간 내에 포함되는지 확인합니다.
     * 거래소 API의 created_at 형식: "2025-01-01T12:00:00+09:00" (ISO 8601)
     */
    private boolean isWithinPeriod(String createdAt, LocalDate startDate) {
        if (createdAt == null || createdAt.isBlank()) {
            return false;
        }

        try {
            LocalDateTime transactionDateTime;

            // ISO 8601 오프셋 형식 (예: 2025-01-01T12:00:00+09:00)
            try {
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(createdAt);
                transactionDateTime = offsetDateTime.toLocalDateTime();
            } catch (DateTimeParseException e) {
                // 오프셋 없는 형식 (예: 2025-01-01T12:00:00)
                transactionDateTime = LocalDateTime.parse(createdAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }

            LocalDate transactionDate = transactionDateTime.toLocalDate();
            return !transactionDate.isBefore(startDate);

        } catch (DateTimeParseException e) {
            log.warn("거래 시간 파싱 실패 - createdAt: {}", createdAt, e);
            return false;
        }
    }
}
