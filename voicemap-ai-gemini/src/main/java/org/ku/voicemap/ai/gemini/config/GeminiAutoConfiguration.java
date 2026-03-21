package org.ku.voicemap.ai.gemini.config;

import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@AutoConfiguration
@EnableConfigurationProperties(GeminiProperties.class)
@ComponentScan(basePackages = "org.ku.voicemap.ai.gemini")
public class GeminiAutoConfiguration {

    @Bean
    public WebSocketClient geminiWebSocketClient() {
        WsWebSocketContainer container = new WsWebSocketContainer();
        container.setDefaultMaxBinaryMessageBufferSize(1024 * 1024);
        container.setDefaultMaxTextMessageBufferSize(1024 * 1024);
        return new StandardWebSocketClient(container);
    }
}
