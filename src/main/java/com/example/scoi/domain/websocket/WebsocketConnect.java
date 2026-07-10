package com.example.scoi.domain.websocket;

import com.example.scoi.domain.websocket.code.WebsocketErrorCode;
import com.example.scoi.domain.websocket.event.WebsocketConnectionEvent.ConnectedEvent;
import com.example.scoi.domain.websocket.event.WebsocketConnectionEvent.DisconnectedEvent;
import com.example.scoi.domain.websocket.exception.WebsocketException;
import com.example.scoi.domain.websocket.handler.UpbitTickerHandler;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.WebSocketClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebsocketConnect {

    private final WebSocketClient webSocketClient;
    private final UpbitTickerHandler upbitTickerHandler;
    private final ObjectProvider<WebsocketConnect> selfProvider; // AOP 우회 방지를 위한 프록시 의존성

    private static final String PUBLIC_URL = "wss://api.upbit.com/websocket/v1";

    // 워치독(Half-Open 헬스체크) 전용 스케줄러
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> watchdogTask;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("[ Websocket ]: 애플리케이션 준비 완료.");
        selfProvider.getObject().connectWithRetry();
    }

    @Async
    @Retryable(
            retryFor = WebsocketException.class,
            maxAttempts = 10,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 60000)
    )
    public void connectWithRetry() {
        log.info("[ Websocket ]: 업비트 웹소켓 연결 시도 중... URL: {}", PUBLIC_URL);
        try {
            // execute()의 리턴값인 Future를 대기(get)하여 타임아웃이나 연결 거부 시 예외를 발생시키도록 유도
            webSocketClient.execute(upbitTickerHandler, PUBLIC_URL).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("[ Websocket ]: 웹소켓 연결 실패: {}", e.getMessage());
            // 예외를 던져서 @Retryable이 AOP 기반 백오프 및 재시도를 수행하도록 트리거
            throw new WebsocketException(WebsocketErrorCode.UPBIT_WEBSOCKET_ERROR);
        }
    }

    @Recover
    public void recover(Exception e) {
        log.error("[ Websocket ]: 웹소켓 재연결이 최대 시도 횟수(10회)를 초과했습니다.");
    }

    @EventListener
    public void onConnected(ConnectedEvent event) {
        log.info("[ Websocket ]: 이벤트 수신 - 웹소켓 연결 활성화 완료.");
        
        if (watchdogTask != null && !watchdogTask.isCancelled()) {
            watchdogTask.cancel(false);
        }
        // 10초마다 마지막 수신 시간 검사
        watchdogTask = scheduler.scheduleAtFixedRate(() -> {
            long idleTime = System.currentTimeMillis() - upbitTickerHandler.getLastMessageTime();
            if (idleTime > 30_000) { // 30초 초과 시
                log.error("[ Websocket ]: 30초간 업비트 응답이 없습니다...");
                upbitTickerHandler.closeSession(); // DisconnectedEvent 유발
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    @EventListener
    public void onDisconnected(DisconnectedEvent event) {
        log.warn("[ Websocket ]: 이벤트 수신 - 웹소켓 연결 해제. 재연결 시도");
        if (watchdogTask != null) {
            watchdogTask.cancel(false);
        }
        selfProvider.getObject().connectWithRetry();
    }

    @PreDestroy
    public void shutdown() {
        log.info("[ Websocket ]: 워치독 스케줄러 종료 중...");
        scheduler.shutdown();
    }
}
