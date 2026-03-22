package org.ku.voicemap.ai.realtime;

public interface AiRealtimeClient {

    void connect(String sessionId, String systemInstruction, AiAgentListener listener);

    void sendAudio(String sessionId, String base64Audio);

    void sendText(String sessionId, String text);

    void disconnect(String sessionId);
}
