package com.example.scoi.domain.websocket.converter;

import com.example.scoi.domain.websocket.dto.UpbitReqDTO;
import com.example.scoi.domain.websocket.dto.WebSocketReqDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.RequiredArgsConstructor;
import org.springframework.web.socket.TextMessage;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class WebSocketConverter {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);

    // 코인 가격 조회: 업비트
    public static TextMessage toGetCoinPrice(
            List<String> codes
    ) throws JsonProcessingException {
        List<Object> payload = List.of(toTicket(), toTicker(codes) ,toFormat());
        return new TextMessage(objectMapper.writeValueAsString(payload));
    }

    private static WebSocketReqDTO.Ticket toTicket(){
        return WebSocketReqDTO.Ticket.builder()
                .ticket(UUID.randomUUID().toString())
                .build();
    }

    private static WebSocketReqDTO.Format toFormat(){
        return WebSocketReqDTO.Format.builder()
                .format("SIMPLE_LIST")
                .build();
    }

    private static UpbitReqDTO.Ticker toTicker(
            List<String> codes
    ){
        return UpbitReqDTO.Ticker.builder()
                .type("ticker")
                .codes(codes)
                .build();
    }
}
