package com.example.scoi.domain.transfer.utils;

import java.util.regex.Pattern;

public class WalletUtils {

    // Ethereum (ETH) & KAIA 정규표현식
    private static final Pattern ETH_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$");

    // Tron (TRX) 정규표현식
    private static final Pattern TRON_PATTERN = Pattern.compile("^T[1-9A-HJ-NP-Za-km-z]{33}$");

    // Aptos 정규표현식: 0x + 1~64자리 16진수 (표준은 64자리)
    private static final Pattern APTOS_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{1,64}$");

    /**
     * 지갑 주소의 유효성을 검사합니다.
     * @param address 검증할 지갑 주소
     * @return 유효한 ETH 또는 TRX 주소이면 true
     */
    public static boolean isValidAddress(String address) {
        return ETH_PATTERN.matcher(address).matches()
                || TRON_PATTERN.matcher(address).matches()
                || APTOS_PATTERN.matcher(address).matches();
    }
}
