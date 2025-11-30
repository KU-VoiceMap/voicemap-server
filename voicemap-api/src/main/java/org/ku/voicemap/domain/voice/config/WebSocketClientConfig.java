package org.ku.voicemap.domain.voice.config;

import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Configuration
public class WebSocketClientConfig {

    @Bean
    public WebSocketClient webSocketClient() {
        WsWebSocketContainer container = new WsWebSocketContainer();
        return new StandardWebSocketClient(container);
    }
}
