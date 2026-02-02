package com.example.scoi.domain.member.enums;

import com.example.scoi.domain.charge.exception.ChargeException;
import com.example.scoi.domain.charge.exception.code.ChargeErrorCode;

public enum ExchangeType {
    BITHUMB("Bithumb"),
    UPBIT("Upbit"),
    BINANCE("Binance");
    
    private final String displayName;
    
    ExchangeType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    
    //문자열을 ExchangeType enum으로 변환
     
    public static ExchangeType fromString(String value) {
        for (ExchangeType type : values()) {
            if (type.displayName.equalsIgnoreCase(value)) {
                return type;
            }
        }
        // IllegalArgumentException 대신 ChargeException
        throw new ChargeException(ChargeErrorCode.WRONG_EXCHANGE_TYPE);
    }
}
