package org.ku.voicemap.domain.ephemeralToken.dto;

public record CreateEphemeralRequest(
    int uses, int expireMinutes, int sessionExpireMinutes
) {
}
