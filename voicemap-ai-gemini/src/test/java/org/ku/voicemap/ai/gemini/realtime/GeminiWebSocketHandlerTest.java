package org.ku.voicemap.ai.gemini.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ku.voicemap.ai.realtime.AiAgentListener;
import org.ku.voicemap.ai.realtime.AiRole;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.Mockito.*;

class GeminiWebSocketHandlerTest {

    private AiAgentListener listener;
    private Consumer<String> onResumptionHandle;
    private GeminiWebSocketHandler handler;
    private WebSocketSession session;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        listener = mock(AiAgentListener.class);
        onResumptionHandle = mock(Consumer.class);
        Runnable onDisconnect = mock(Runnable.class);
        handler = new GeminiWebSocketHandler(
            "test-session", new ObjectMapper(), listener, onDisconnect, onResumptionHandle
        );
        session = mock(WebSocketSession.class);
    }

    @Test
    void setupComplete_callsOnSessionReady() throws Exception {
        sendMessage("""
            {"setupComplete": {}}
            """);

        verify(listener).onSessionReady("test-session");
        verifyNoMoreInteractions(listener);
    }

    @Test
    void inputTranscription_callsOnTranscriptWithUserRole() throws Exception {
        sendMessage("""
            {"serverContent": {"inputTranscription": {"text": "안녕하세요"}}}
            """);

        verify(listener).onTranscript("test-session", AiRole.USER, "안녕하세요");
    }

    @Test
    void outputTranscription_callsOnTranscriptWithAgentRole() throws Exception {
        sendMessage("""
            {"serverContent": {"outputTranscription": {"text": "반갑습니다"}}}
            """);

        verify(listener).onTranscript("test-session", AiRole.AGENT, "반갑습니다");
    }

    @Test
    void interrupted_callsOnInterrupted() throws Exception {
        sendMessage("""
            {"serverContent": {"interrupted": true}}
            """);

        verify(listener).onInterrupted("test-session");
    }

    @Test
    void modelTurnWithAudio_callsOnAudioOutput() throws Exception {
        sendMessage("""
            {"serverContent": {"modelTurn": {"parts": [{"inlineData": {"data": "base64audio", "mimeType": "audio/pcm"}}]}}}
            """);

        verify(listener).onAudioOutput("test-session", "base64audio");
    }

    @Test
    void turnComplete_callsOnTurnComplete() throws Exception {
        sendMessage("""
            {"serverContent": {"turnComplete": true}}
            """);

        verify(listener).onTurnComplete("test-session");
    }

    @Test
    void sessionResumptionUpdate_callsOnResumptionHandle() throws Exception {
        sendMessage("""
            {"sessionResumptionUpdate": {"resumable": true, "newHandle": "handle-abc"}}
            """);

        verify(onResumptionHandle).accept("handle-abc");
    }

    @Test
    void sessionResumptionUpdate_notResumable_doesNotCallOnResumptionHandle() throws Exception {
        sendMessage("""
            {"sessionResumptionUpdate": {"resumable": false, "newHandle": "handle-abc"}}
            """);

        verifyNoInteractions(onResumptionHandle);
    }

    private void sendMessage(String json) throws Exception {
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(json);
        handler.handleMessage(session, new BinaryMessage(buffer));
    }
}
