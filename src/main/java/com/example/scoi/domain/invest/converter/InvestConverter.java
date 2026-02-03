package com.example.scoi.domain.invest.converter;

import com.example.scoi.domain.member.enums.ExchangeType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class InvestConverter implements Converter<String, ExchangeType> {
    
    @Override
    public ExchangeType convert(String source) {
        try {
            return ExchangeType.fromString(source);
        } catch (IllegalArgumentException e) {
            // 변환 실패 시 IllegalArgumentException을 그대로 던짐
            // GeneralExceptionAdvice에서 처리
            throw e;
        }
    }
}
