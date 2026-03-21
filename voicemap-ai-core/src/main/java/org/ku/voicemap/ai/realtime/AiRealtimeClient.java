package org.ku.voicemap.ai.realtime;

public interface AiRealtimeClient {

    void connect(String sessionId, String systemInstruction, AiAgentListener listener);

    void sendAudio(String sessionId, String base64Audio);

    void disconnect(String sessionId);
}
