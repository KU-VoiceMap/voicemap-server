package org.ku.voicemap.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.ku.voicemap.domain.auth.AuthProvider;

public record TokenRequest(
    @NotNull AuthProvider provider,
    @NotBlank String providerToken
) {
}
