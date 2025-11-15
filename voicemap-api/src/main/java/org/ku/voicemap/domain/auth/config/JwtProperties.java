package org.ku.voicemap.domain.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String secretKey,
    Duration accessTokenExpireDuration,
    Duration refreshTokenExpireDuration
) {
}
