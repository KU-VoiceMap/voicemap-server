package org.ku.voicemap.domain.auth.token;

import java.time.LocalDateTime;

public record TokenPair(
    String accessToken,
    String refreshToken,
    LocalDateTime expireAt
) {
}
