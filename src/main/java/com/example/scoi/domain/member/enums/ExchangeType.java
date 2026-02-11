package com.example.scoi.domain.member.enums;

import com.example.scoi.domain.charge.exception.ChargeException;
import com.example.scoi.domain.charge.exception.code.ChargeErrorCode;

public enum ExchangeType {
    BITHUMB("Bithumb", "빗썸"),
    UPBIT("Upbit", "업비트");

    private final String displayName;
    private final String koreanName;

    ExchangeType(String displayName, String koreanName) {

        this.displayName = displayName;
        this.koreanName = koreanName;
    }

    public String getDisplayName() {
        return displayName;
    }


    //문자열을 ExchangeType enum으로 변환

    public static ExchangeType fromString(String value) {
        for (ExchangeType type : values()) {
            if (type.displayName.equalsIgnoreCase(value) || type.koreanName.equals(value)) {
                return type;
            }
        }
        // IllegalArgumentException 대신 ChargeException
        throw new ChargeException(ChargeErrorCode.WRONG_EXCHANGE_TYPE);
    }
}
