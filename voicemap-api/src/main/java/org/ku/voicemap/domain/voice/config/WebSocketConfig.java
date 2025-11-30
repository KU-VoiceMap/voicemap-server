package org.ku.voicemap.domain.voice.config;

import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.voice.inbound.ClientWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ClientWebSocketHandler clientWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(clientWebSocketHandler, "/ws/chat")
            .setAllowedOrigins("*");
    }
}
