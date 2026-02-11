package com.example.scoi.domain.websocket.handler;

import com.example.scoi.domain.websocket.converter.WebSocketConverter;
import com.example.scoi.domain.websocket.dto.UpbitResDTO;
import com.example.scoi.domain.websocket.service.WebSocketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UpbitTickerHandler extends BinaryWebSocketHandler {

    private final SimpMessageSendingOperations simpMessageSendingOperations;
    private final WebSocketService webSocketService;

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);

    @Override
    public void afterConnectionEstablished(
            WebSocketSession session
    ) throws IOException {
        session.sendMessage(WebSocketConverter.toGetCoinPrice(List.of("KRW-USDT","KRW-USDC")));
    }

    @Override
    public void handleBinaryMessage(
            WebSocketSession session,
            BinaryMessage message
    ) throws IOException {
        String converted = new String(message.getPayload().array(), StandardCharsets.UTF_8);
        converted = converted.replace("[","").replace("]","");
        publish(converted);
        UpbitResDTO.Ticker dto = objectMapper.readValue(converted, UpbitResDTO.Ticker.class);
        webSocketService.ticker(dto);
    }

    @Async
    protected void publish(String message) {
        simpMessageSendingOperations.convertAndSend("/topic/ticker", message);
    }
}
