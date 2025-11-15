package org.ku.voicemap.domain.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import org.ku.voicemap.domain.auth.config.JwtProperties;
import org.ku.voicemap.domain.auth.entity.Token;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class TokenProvider {

    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    private final Algorithm algorithm;
    private final Duration accessTokenExpireDuration;
    private final Duration refreshTokenExpireDuration;
    private final JWTVerifier tokenVerifier;

    public TokenProvider(JwtProperties jwtProperties) {
        this.algorithm = Algorithm.HMAC256(jwtProperties.secretKey());
        this.accessTokenExpireDuration = jwtProperties.accessTokenExpireDuration();
        this.refreshTokenExpireDuration = jwtProperties.refreshTokenExpireDuration();
        this.tokenVerifier = JWT.require(algorithm).withClaimPresence("memberNumber").build();
    }

    public Token generateToken(String memberNumber) {
        String accessToken = generateAccessToken(memberNumber);
        return generateRefreshToken(accessToken, memberNumber);
    }

    public String generateAccessToken(String memberNumber) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expirationTime = now.plus(accessTokenExpireDuration);
        return JWT.create()
            .withClaim("memberNumber", memberNumber)
            .withClaim("type", "ACCESS")
            .withIssuedAt(now.toInstant(KST))
            .withExpiresAt(expirationTime.toInstant(KST))
            .sign(algorithm);
    }

    public Token generateRefreshToken(String accessToken, String memberNumber) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expirationTime = now.plus(refreshTokenExpireDuration);
        String refreshToken = JWT.create()
            .withClaim("memberNumber", memberNumber)
            .withClaim("type", "REFRESH")
            .withIssuedAt(now.toInstant(KST))
            .withExpiresAt(expirationTime.toInstant(KST))
            .sign(algorithm);
        return new Token(accessToken, refreshToken, now, expirationTime);
    }

    public boolean validateToken(String token) {
        try {
            DecodedJWT decodedToken = tokenVerifier.verify(token);
            return decodedToken.getExpiresAt().after(new Date());
        } catch (JWTVerificationException e) {
            return false;
        }
    }
}
