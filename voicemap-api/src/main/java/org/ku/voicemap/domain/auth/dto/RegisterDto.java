package org.ku.voicemap.domain.auth.dto;

import org.ku.voicemap.domain.auth.AuthProvider;

public record RegisterDto(String providerId, String email, AuthProvider provider) {
}
