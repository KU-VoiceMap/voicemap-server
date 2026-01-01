package org.ku.voicemap.domain.voice.inbound.payload;

import jakarta.annotation.Nullable;

public record SessionInitPayload(
    String token,
    @Nullable String chatId
) {
}
