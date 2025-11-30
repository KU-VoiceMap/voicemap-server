package org.ku.voicemap.domain.voice.inbound;

import java.util.Arrays;

public enum ClientMessageType {
    SESSION_INIT,
    AUDIO_INPUT,
    ;

    public static ClientMessageType from(String type) {
        return Arrays.stream(ClientMessageType.values())
            .filter(t -> t.name().equalsIgnoreCase(type))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown message type: " + type));
    }
}
