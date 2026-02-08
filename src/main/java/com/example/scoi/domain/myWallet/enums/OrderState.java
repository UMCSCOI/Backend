package com.example.scoi.domain.myWallet.enums;

public enum OrderState {
    DONE,       // 완료
    WAIT,       // 대기
    CANCEL;     // 취소

    /**
     * 거래소 API에 전달할 state 문자열을 반환합니다.
     */
    public String toApiValue() {
        return this.name().toLowerCase();
    }
}
