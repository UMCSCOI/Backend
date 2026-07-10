package com.example.scoi.domain.websocket.handler;

import com.example.scoi.domain.websocket.converter.WebSocketConverter;
import com.example.scoi.domain.websocket.dto.UpbitResDTO;
import com.example.scoi.domain.websocket.event.WebsocketConnectionEvent.ConnectedEvent;
import com.example.scoi.domain.websocket.event.WebsocketConnectionEvent.DisconnectedEvent;
import com.example.scoi.domain.websocket.service.WebSocketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpbitTickerHandler extends BinaryWebSocketHandler {

    private final SimpMessageSendingOperations simpMessageSendingOperations;
    private final WebSocketService webSocketService;
    private final ApplicationEventPublisher eventPublisher;

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);

    private volatile long lastMessageTime;
    private WebSocketSession currentSession;

    @Override
    public void afterConnectionEstablished(
            WebSocketSession session
    ) throws IOException {
        this.currentSession = session;
        this.lastMessageTime = System.currentTimeMillis();
        log.info("[ Websocket ]: 업비트 웹소켓 연결 성공.");
        eventPublisher.publishEvent(new ConnectedEvent());
        session.sendMessage(WebSocketConverter.toGetCoinPrice(List.of("KRW-USDT","KRW-USDC")));
    }

    @Override
    public void handleBinaryMessage(
            WebSocketSession session,
            BinaryMessage message
    ) throws IOException {
        this.lastMessageTime = System.currentTimeMillis(); // Heartbeat 갱신
        
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

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.warn("[ Websocket ]: 업비트 웹소켓 연결이 종료되었습니다. CloseStatus: {}", status);
        eventPublisher.publishEvent(new DisconnectedEvent());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("[ Websocket ]: 웹소켓 에러 발생: {}", exception.getMessage());
        eventPublisher.publishEvent(new DisconnectedEvent());
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void closeSession() {
        if (currentSession != null && currentSession.isOpen()) {
            try {
                log.warn("[ Websocket ]: 하프 오픈(Half-open) 상태 감지.");
                currentSession.close();
            } catch (IOException e) {
                log.error("[ Websocket ]: 세션 강제 종료 중 에러 발생: {}", e.getMessage());
            }
        }
    }
}
