package com.example.scoi.domain.websocket.event;

public interface WebsocketConnectionEvent {
    record ConnectedEvent() implements WebsocketConnectionEvent {}
    record DisconnectedEvent() implements WebsocketConnectionEvent {}
}
