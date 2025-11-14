package org.ku.voicemap.domain.ephemeralToken.dto;

import java.time.LocalDateTime;
import org.ku.voicemap.domain.ephemeralToken.entity.EphemeralToken;

public record EphemeralTokenResponse(
    Long userId,
    int uses,
    LocalDateTime createdAt,
    int expireMinutes,
    int sessionExpireMinutes,
    String ephemeralToken
) {

    public static EphemeralTokenResponse toDto(EphemeralToken entity) {
        return new EphemeralTokenResponse(
            entity.getUserId(),
            entity.getUses(),
            entity.getCreatedAt(),
            entity.getExpireMinutes(),
            entity.getSessionExpireMinutes(),
            entity.getEphemeralToken()
        );
    }
}
