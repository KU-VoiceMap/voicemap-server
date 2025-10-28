package org.ku.voicemap.domain.oauth.dto;

import org.ku.voicemap.domain.member.entity.Provider;

public record RegisterDto(
    String providerId, String email, Provider provider
) {

}
