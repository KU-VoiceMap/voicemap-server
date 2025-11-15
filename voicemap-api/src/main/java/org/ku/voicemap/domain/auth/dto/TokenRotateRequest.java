package org.ku.voicemap.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRotateRequest(
    @NotBlank String refreshToken
) {
}
