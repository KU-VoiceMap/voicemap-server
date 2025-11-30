package org.ku.voicemap.domain.voice.ai;

public interface AgentConnector {

    void connect(String sessionId);

    void sendAudio(String sessionId, String base64Audio);

    void disconnect(String sessionId);
}
