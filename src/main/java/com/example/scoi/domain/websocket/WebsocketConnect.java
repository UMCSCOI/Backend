package com.example.scoi.domain.websocket;

import com.example.scoi.domain.websocket.handler.UpbitTickerHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.WebSocketClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebsocketConnect {

    private final WebSocketClient webSocketClient;
    private final UpbitTickerHandler upbitTickerHandler;

    private static final String PUBLIC_URL = "wss://api.upbit.com/websocket/v1";

    // 실시간 가격 변동 체크
    @EventListener(ApplicationReadyEvent.class)
    public void connect(){
        log.info("[ Websocket ]: 디페깅 알고리즘 구동 시작...");
        webSocketClient.execute(upbitTickerHandler, PUBLIC_URL);
    }
}
