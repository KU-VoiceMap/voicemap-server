package org.ku.voicemap.domain.auth.dto;

import org.ku.voicemap.domain.auth.OAuthProvider;

public record RegisterDto(
    String providerId, String email, OAuthProvider provider
) {

}
