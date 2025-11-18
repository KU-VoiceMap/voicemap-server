package org.ku.voicemap.domain.oauth.dto;

import org.ku.voicemap.domain.member.model.Provider;

public record RegisterDto(
    String providerId, String email, Provider provider
) {

}
