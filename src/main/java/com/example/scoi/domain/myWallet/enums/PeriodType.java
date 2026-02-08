package com.example.scoi.domain.myWallet.enums;

import java.time.LocalDate;

public enum PeriodType {
    TODAY,
    ONE_MONTH,
    THREE_MONTHS,
    SIX_MONTHS;

    /**
     * 기간 시작일을 반환합니다.
     * TODAY: 오늘 00:00:00
     * ONE_MONTH: 1개월 전
     * THREE_MONTHS: 3개월 전
     * SIX_MONTHS: 6개월 전
     */
    public LocalDate getStartDate() {
        LocalDate today = LocalDate.now();
        return switch (this) {
            case TODAY -> today;
            case ONE_MONTH -> today.minusMonths(1);
            case THREE_MONTHS -> today.minusMonths(3);
            case SIX_MONTHS -> today.minusMonths(6);
        };
    }
}
