package com.example.scoi.domain.transfer.utils;

import com.example.scoi.domain.transfer.exception.TransferException;
import com.example.scoi.domain.transfer.exception.code.TransferErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TransferCursorUtils {

    // 날짜 포맷 (나노초까지 포함하여 정밀도 유지)
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String SEPARATOR = "_";

    // 인코딩: LocalDateTime + TradeHistoryID -> Base64 String
    public static String encode(LocalDateTime timestamp, Long id) {
        if (timestamp == null || id == null) return null;
        String rawCursor = timestamp.format(formatter) + SEPARATOR + id;
        return Base64.getEncoder().encodeToString(rawCursor.getBytes());
    }

    // 디코딩: Base64 String -> 내부 객체 (Timestamp, ID)
    public static CursorContents decode(String cursor) {
        if (cursor == null || cursor.isEmpty()) return null;

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(cursor);
            String rawCursor = new String(decodedBytes);
            String[] parts = rawCursor.split(SEPARATOR);

            LocalDateTime timestamp = LocalDateTime.parse(parts[0], formatter);
            Long id = Long.parseLong(parts[1]);

            return new CursorContents(timestamp, id);
        } catch (Exception e) {
            // 커서 형식이 올바르지 않을 때 예외 처리
            throw new TransferException(TransferErrorCode.INVALID_CURSOR);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class CursorContents {
        private LocalDateTime timestamp;
        private Long id;
    }
}