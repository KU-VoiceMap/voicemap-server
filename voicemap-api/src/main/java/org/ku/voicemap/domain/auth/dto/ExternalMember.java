package org.ku.voicemap.domain.auth.dto;

import org.ku.voicemap.domain.auth.AuthProvider;

public record ExternalMember(AuthProvider provider, String principal, String email) {
}
