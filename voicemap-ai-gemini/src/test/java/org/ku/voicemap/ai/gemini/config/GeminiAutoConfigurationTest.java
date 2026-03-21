package org.ku.voicemap.ai.gemini.config;

import org.junit.jupiter.api.Test;
import org.ku.voicemap.ai.chat.AiChatClient;
import org.ku.voicemap.ai.gemini.chat.GeminiChatClient;
import org.ku.voicemap.ai.gemini.realtime.GeminiRealtimeClient;
import org.ku.voicemap.ai.realtime.AiRealtimeClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.socket.client.WebSocketClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    classes = {GeminiAutoConfiguration.class, JacksonAutoConfiguration.class},
    properties = {
        "gemini.api-key=test-key",
        "gemini.urls.agent-web-socket-url=wss://test/ws",
        "gemini.urls.document-api-url=https://test/doc",
        "gemini.urls.chat-api-url=https://test/chat",
        "gemini.models.agent-model=test-agent",
        "gemini.models.document-model=test-doc",
        "gemini.models.chat-model=test-chat"
    }
)
class GeminiAutoConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void registersGeminiPropertiesWithCorrectValues() {
        GeminiProperties properties = context.getBean(GeminiProperties.class);

        assertThat(properties.apiKey()).isEqualTo("test-key");
        assertThat(properties.urls().agentWebSocketUrl()).isEqualTo("wss://test/ws");
        assertThat(properties.urls().documentApiUrl()).isEqualTo("https://test/doc");
        assertThat(properties.urls().chatApiUrl()).isEqualTo("https://test/chat");
        assertThat(properties.models().agentModel()).isEqualTo("test-agent");
        assertThat(properties.models().documentModel()).isEqualTo("test-doc");
        assertThat(properties.models().chatModel()).isEqualTo("test-chat");
    }

    @Test
    void registersWebSocketClientBean() {
        assertThat(context.getBean(WebSocketClient.class)).isNotNull();
    }

    @Test
    void registersAiChatClientAsGeminiChatClient() {
        AiChatClient chatClient = context.getBean(AiChatClient.class);

        assertThat(chatClient).isInstanceOf(GeminiChatClient.class);
    }

    @Test
    void registersAiRealtimeClientAsGeminiRealtimeClient() {
        AiRealtimeClient realtimeClient = context.getBean(AiRealtimeClient.class);

        assertThat(realtimeClient).isInstanceOf(GeminiRealtimeClient.class);
    }
}
