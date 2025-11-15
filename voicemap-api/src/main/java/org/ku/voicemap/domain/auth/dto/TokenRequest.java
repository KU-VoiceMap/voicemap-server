package org.ku.voicemap.domain.auth.dto;

import org.ku.voicemap.domain.auth.AuthProvider;

public record TokenRequest(String email, AuthProvider provider, String principal) {
}
