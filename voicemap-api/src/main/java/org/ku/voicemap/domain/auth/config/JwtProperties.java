package org.ku.voicemap.domain.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    String secretKey,
    Long accessToken,
    Long refreshToken
) {
}
