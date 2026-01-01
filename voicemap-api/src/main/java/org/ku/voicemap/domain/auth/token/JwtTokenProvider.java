package org.ku.voicemap.domain.auth.token;

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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class JwtTokenProvider implements TokenProvider {

    private static final ZoneOffset KST = ZoneOffset.ofHours(9);
    private static final String CLAIM_MEMBER_NUMBER = "memberNumber";

    private final Algorithm algorithm;
    private final Duration accessTokenExpireDuration;
    private final Duration refreshTokenExpireDuration;
    private final JWTVerifier tokenVerifier;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.algorithm = Algorithm.HMAC256(jwtProperties.secretKey());
        this.accessTokenExpireDuration = jwtProperties.accessTokenExpireDuration();
        this.refreshTokenExpireDuration = jwtProperties.refreshTokenExpireDuration();
        this.tokenVerifier = JWT.require(algorithm).withClaimPresence(CLAIM_MEMBER_NUMBER).build();
    }

    @Override
    public TokenPair generateTokenPair(String memberNumber, LocalDateTime now) {
        LocalDateTime accessTokenExpireAt = now.plus(accessTokenExpireDuration);
        LocalDateTime refreshTokenExpireAt = now.plus(refreshTokenExpireDuration);
        String accessToken = generateToken(memberNumber, "ACCESS", now, accessTokenExpireAt);
        String refreshToken = generateToken(memberNumber, "REFRESH", now, refreshTokenExpireAt);
        return new TokenPair(accessToken, refreshToken, refreshTokenExpireAt);
    }

    @Override
    public String generateAccessToken(String memberNumber, LocalDateTime now) {
        LocalDateTime accessTokenExpireAt = now.plus(accessTokenExpireDuration);
        return generateToken(memberNumber, "ACCESS", now, accessTokenExpireAt);
    }

    private String generateToken(String memberNumber, String type, LocalDateTime issuedAt, LocalDateTime expireAt) {
        return JWT.create()
            .withClaim(CLAIM_MEMBER_NUMBER, memberNumber)
            .withClaim("type", type)
            .withIssuedAt(issuedAt.toInstant(KST))
            .withExpiresAt(expireAt.toInstant(KST))
            .sign(algorithm);
    }

    @Override
    public boolean validateToken(String token) {
        try {
            DecodedJWT decodedToken = tokenVerifier.verify(token);
            return decodedToken.getExpiresAt().after(new Date());
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    @Override
    public String extractMemberNumber(String token) {
        DecodedJWT decodedToken = tokenVerifier.verify(token);
        return decodedToken.getClaim(CLAIM_MEMBER_NUMBER).asString();
    }
}
