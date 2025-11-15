package org.ku.voicemap.domain.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.InvalidKeyException;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.config.JwtProperties;
import org.ku.voicemap.domain.auth.entity.Token;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenProvider {

    private final JwtProperties jwtProperties;

    @Transactional
    public Token generateToken(String memberNumber) {
        String accessToken = generateAccessToken(memberNumber);
        return generateRefreshToken(accessToken, memberNumber);
    }

    public String generateAccessToken(String memberNumber) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expirationTime = now.plus(jwtProperties.accessToken(), ChronoUnit.MILLIS);

        Date dateNow = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        Date dateExp = Date.from(expirationTime.atZone(ZoneId.systemDefault()).toInstant());
        try {
            return Jwts.builder()
                .claim("memberNumber", memberNumber)
                .claim("type", "ACCESS")
                .setIssuedAt(dateNow)
                .setExpiration(dateExp)
                .signWith(getSigningKey())
                .compact();
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }

    public Token generateRefreshToken(String accessToken, String memberNumber) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expirationTime = now.plus(jwtProperties.refreshToken(), ChronoUnit.MILLIS);

        Date dateNow = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        Date dateExp = Date.from(expirationTime.atZone(ZoneId.systemDefault()).toInstant());

        try {
            String refreshToken = Jwts.builder()
                .claim("memberNumber", memberNumber)
                .claim("type", "REFRESH")
                .setIssuedAt(dateNow)
                .setExpiration(dateExp)
                .signWith(getSigningKey())
                .compact();
            return new Token(accessToken, refreshToken, now, expirationTime);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException();
        }
    }

    public boolean validateToken(String token) {
        // TODO: 토큰 내부 검사
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secretKey().getBytes());
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);

        // "type" 클레임 검사 (매우 중요)
        if (!"ACCESS".equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException();
        }

        String memberId = claims.get("memberId", String.class);

        return new UsernamePasswordAuthenticationToken(memberId, null, Collections.emptyList());
    }
}
