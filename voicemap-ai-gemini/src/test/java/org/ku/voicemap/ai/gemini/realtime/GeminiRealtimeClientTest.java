package org.ku.voicemap.ai.gemini.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ku.voicemap.ai.gemini.config.GeminiProperties;
import org.ku.voicemap.ai.realtime.AiAgentListener;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class GeminiRealtimeClientTest {

    private WebSocketClient webSocketClient;
    private GeminiProperties geminiProperties;
    private GeminiRealtimeClient client;
    private AiAgentListener listener;

    @BeforeEach
    void setUp() {
        webSocketClient = mock(WebSocketClient.class);
        geminiProperties = new GeminiProperties(
            "test-key",
            new GeminiProperties.Urls("wss://gemini.test/ws", "https://doc.test", "https://chat.test"),
            new GeminiProperties.Models("models/test-agent", "doc-model", "chat-model"),
            new GeminiProperties.SystemInstructions("agent-inst", "doc-inst", "chat-inst", "summarizer-inst")
        );
        client = new GeminiRealtimeClient(webSocketClient, geminiProperties, new ObjectMapper());
        listener = mock(AiAgentListener.class);
    }

    @Test
    void connect_callsWebSocketClientWithCorrectUrl() {
        when(webSocketClient.execute(any(WebSocketHandler.class), any(String.class)))
            .thenReturn(new CompletableFuture<>());

        client.connect("session-1", "test instruction", listener);

        verify(webSocketClient).execute(
            any(GeminiWebSocketHandler.class),
            contains("wss://gemini.test/ws?key=test-key")
        );
    }

    @Test
    void sendAudio_withUnknownSession_throwsException() {
        assertThatThrownBy(() -> client.sendAudio("unknown", "audio"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unknown");
    }

    @Test
    void disconnect_closesConnectionAndRemovesSession() throws Exception {
        WebSocketSession mockSession = mock(WebSocketSession.class);
        ConcurrentHashMap<String, GeminiRealtimeClient.SessionState> sessions = new ConcurrentHashMap<>();
        sessions.put("session-1", new GeminiRealtimeClient.SessionState(mockSession, "inst", listener, null));

        client = new GeminiRealtimeClient(webSocketClient, geminiProperties, new ObjectMapper(), sessions);
        client.disconnect("session-1");

        verify(mockSession).close();
    }

    @Test
    void disconnect_withNullConnection_doesNotThrow() {
        ConcurrentHashMap<String, GeminiRealtimeClient.SessionState> sessions = new ConcurrentHashMap<>();
        sessions.put("session-1", new GeminiRealtimeClient.SessionState(null, "inst", listener, null));

        client = new GeminiRealtimeClient(webSocketClient, geminiProperties, new ObjectMapper(), sessions);
        client.disconnect("session-1");
    }

    @Test
    void sendAudio_sendsMessageOnConnection() throws Exception {
        WebSocketSession mockSession = mock(WebSocketSession.class);
        ConcurrentHashMap<String, GeminiRealtimeClient.SessionState> sessions = new ConcurrentHashMap<>();
        sessions.put("session-1", new GeminiRealtimeClient.SessionState(mockSession, "inst", listener, null));

        client = new GeminiRealtimeClient(webSocketClient, geminiProperties, new ObjectMapper(), sessions);
        client.sendAudio("session-1", "base64data");

        verify(mockSession).sendMessage(any());
    }

    @Test
    void sessionState_withConnection_preservesOtherFields() {
        var state = new GeminiRealtimeClient.SessionState(null, "inst", listener, "handle");
        WebSocketSession mockSession = mock(WebSocketSession.class);
        var updated = state.withConnection(mockSession);

        assertThat(updated.connection()).isEqualTo(mockSession);
        assertThat(updated.systemInstruction()).isEqualTo("inst");
        assertThat(updated.listener()).isEqualTo(listener);
        assertThat(updated.resumptionHandle()).isEqualTo("handle");
    }

    @Test
    void sessionState_withResumptionHandle_preservesOtherFields() {
        WebSocketSession mockSession = mock(WebSocketSession.class);
        var state = new GeminiRealtimeClient.SessionState(mockSession, "inst", listener, null);
        var updated = state.withResumptionHandle("new-handle");

        assertThat(updated.connection()).isEqualTo(mockSession);
        assertThat(updated.systemInstruction()).isEqualTo("inst");
        assertThat(updated.listener()).isEqualTo(listener);
        assertThat(updated.resumptionHandle()).isEqualTo("new-handle");
    }
}
