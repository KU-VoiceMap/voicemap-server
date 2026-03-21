package org.ku.voicemap.ai.realtime;

public interface AiAgentListener {

    void onSessionReady(String sessionId);

    void onTranscript(String sessionId, AiRole role, String text);

    void onAudioOutput(String sessionId, String base64Audio);

    void onInterrupted(String sessionId);

    void onTurnComplete(String sessionId);
}
