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
import org.ku.voicemap.domain.auth.entity.AuthClient;
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

    public Token generateToken(AuthClient authClient) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime accessTokenExpireAt = now.plus(accessTokenExpireDuration);
        LocalDateTime refreshTokenExpireAt = now.plus(refreshTokenExpireDuration);
        String accessToken = generateToken(authClient.getMemberNumber(), "ACCESS", accessTokenExpireAt);
        String refreshToken = generateToken(authClient.getMemberNumber(), "REFRESH", refreshTokenExpireAt);
        return new Token(authClient, accessToken, refreshToken, now, refreshTokenExpireAt);
    }

    public String generateAccessToken(String memberNumber, LocalDateTime now) {
        return generateToken(memberNumber, "ACCESS", now.plus(accessTokenExpireDuration));
    }

    private String generateToken(String memberNumber, String type, LocalDateTime expireAt) {
        LocalDateTime now = LocalDateTime.now();
        return JWT.create()
            .withClaim("memberNumber", memberNumber)
            .withClaim("type", type)
            .withIssuedAt(now.toInstant(KST))
            .withExpiresAt(expireAt.toInstant(KST))
            .sign(algorithm);
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
