package org.ku.voicemap.domain.oauth.dto;

import org.ku.voicemap.domain.ephemeralToken.dto.EphemeralTokenResponse;

public record AuthResponse(String accessToken, String refreshToken, EphemeralTokenResponse ephemeralTokenResponse) {

}
