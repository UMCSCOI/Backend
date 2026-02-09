package com.example.scoi.domain.myWallet.client;

import com.example.scoi.domain.myWallet.dto.MyWalletResDTO;
import com.example.scoi.domain.myWallet.enums.OrderState;
import com.example.scoi.domain.myWallet.enums.PeriodType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 여러 거래소 어댑터를 한 타입으로 묶기 위한 인터페이스
 */
public interface MyWalletExchangeClient {

    /**
     * 현재 계좌 잔고를 조회합니다. (USDT, USDC)
     * @param phoneNumber 사용자 휴대폰 번호
     * @return 통화별 잔고 (balance + locked 합산)
     */
    Map<String, BigDecimal> getBalances(String phoneNumber);

    /**
     * 코인 입금 리스트를 조회합니다. (USDT + USDC)
     * @param phoneNumber 사용자 휴대폰 번호
     * @return 입금 거래 내역 리스트
     */
    List<MyWalletResDTO.TransactionDTO> getDeposits(String phoneNumber);

    /**
     * 코인 출금 리스트를 조회합니다. (USDT + USDC)
     * @param phoneNumber 사용자 휴대폰 번호
     * @return 출금 거래 내역 리스트
     */
    List<MyWalletResDTO.TransactionDTO> getWithdraws(String phoneNumber);

    /**
     * 주문 리스트를 조회합니다. (KRW-USDT + KRW-USDC 충전 거래 내역)
     * @param phoneNumber 사용자 휴대폰 번호
     * @param state       주문 상태 (DONE, WAIT, CANCEL)
     * @param periodType  조회 기간
     * @param order       정렬 순서 (desc: 최신순, asc: 과거순)
     * @param limit       최대 조회 건수
     * @return 충전 거래 내역 리스트
     */
    List<MyWalletResDTO.TopupTransactionDTO> getOrders(
            String phoneNumber,
            OrderState state,
            PeriodType periodType,
            String order,
            int limit
    );

    /**
     * 개별 입금 내역을 조회합니다.
     * @param phoneNumber 사용자 휴대폰 번호
     * @param uuid        입금 UUID
     * @param currency    통화 코드 (USDT, USDC 등, nullable)
     * @return 입금 상세 정보
     */
    MyWalletResDTO.RemitDetailDTO getDepositDetail(String phoneNumber, String uuid, String currency);

    /**
     * 개별 출금 내역을 조회합니다.
     * @param phoneNumber 사용자 휴대폰 번호
     * @param uuid        출금 UUID
     * @param currency    통화 코드 (USDT, USDC 등, nullable)
     * @return 출금 상세 정보
     */
    MyWalletResDTO.RemitDetailDTO getWithdrawDetail(String phoneNumber, String uuid, String currency);

    /**
     * 개별 주문 내역을 조회합니다.
     * @param phoneNumber 사용자 휴대폰 번호
     * @param uuid        주문 UUID
     * @return 주문 상세 정보 (체결 내역 포함)
     */
    MyWalletResDTO.TopupDetailDTO getOrderDetail(String phoneNumber, String uuid);

    /**
     * 원화(KRW) 자산을 조회합니다.
     * @param phoneNumber 사용자 휴대폰 번호
     * @return KRW 잔고 정보 (balance, locked)
     */
    MyWalletResDTO.KrwBalanceDTO getKrwBalance(String phoneNumber);

    /**
     * 원화(KRW) 출금을 요청합니다.
     * @param phoneNumber  사용자 휴대폰 번호
     * @param amount       출금 금액
     * @param mfaType      2차 인증 수단 (kakao, naver, hana)
     * @return 출금 요청 결과 (currency, uuid, txid)
     */
    MyWalletResDTO.WithdrawKrwDTO withdrawKrw(String phoneNumber, Long amount, String mfaType);
}
