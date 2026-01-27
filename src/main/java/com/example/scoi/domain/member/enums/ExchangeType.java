package com.example.scoi.domain.member.enums;

public enum ExchangeType {
    BITHUMB("Bithumb"),
    UPBIT("Upbit");
    
    private final String displayName;
    
    ExchangeType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 문자열을 ExchangeType enum으로 변환
     * 대소문자 구분 없이 변환 가능
     */
    public static ExchangeType fromString(String value) {
        for (ExchangeType type : values()) {
            if (type.displayName.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid exchange type: " + value);
    }
}
