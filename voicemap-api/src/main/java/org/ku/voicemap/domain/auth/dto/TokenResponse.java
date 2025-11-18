package org.ku.voicemap.domain.auth.dto;

public record TokenResponse(String accessToken, String refreshToken) {
}
