package org.ku.voicemap.domain.jwt;

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
import org.ku.voicemap.domain.member.entity.MemberDto;
import org.ku.voicemap.domain.member.service.MemberServiceInter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberServiceInter memberServiceInter;


    @Transactional
    public TokenInfo generateToken(MemberDto member) {

        String accessToken = generateAccessToken(member);

        RefreshToken refreshToken = generateRefreshToken(accessToken, member.id());
        refreshTokenRepository.save(refreshToken);

        return new TokenInfo(accessToken, refreshToken.getRefreshToken());

    }

    @Transactional
    public TokenInfo rotateAccessToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByRefreshToken(token)
            .orElseThrow(IllegalArgumentException::new);

        if (!refreshToken.isPossible()) {
            throw new IllegalArgumentException();
        }

        Long memberId = refreshToken.getMemberId();
        MemberDto memberDto = memberServiceInter.findMember(memberId);
        String newAccessToken = generateAccessToken(memberDto);
        refreshToken.updateAccessToken(newAccessToken);

        return new TokenInfo(newAccessToken, refreshToken.getRefreshToken());
    }

    @Transactional
    public TokenInfo rotateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByRefreshToken(token)
            .orElseThrow(IllegalArgumentException::new);

        if (!refreshToken.isPossible()) {
            throw new IllegalArgumentException();
        }
        refreshToken.updatePossible();
        Long memberId = refreshToken.getMemberId();
        MemberDto memberDto = memberServiceInter.findMember(memberId);
        String newAccessToken = generateAccessToken(memberDto);
        RefreshToken newRefreshToken = generateRefreshToken(newAccessToken, memberId);
        refreshTokenRepository.save(newRefreshToken);
        return new TokenInfo(newAccessToken, newRefreshToken.getRefreshToken());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {

        }
        return false;
    }


    private String generateAccessToken(MemberDto member) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expirationTime = now.plus(jwtProperties.accessToken(), ChronoUnit.MILLIS);

        Date dateNow = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        Date dateExp = Date.from(expirationTime.atZone(ZoneId.systemDefault()).toInstant());
        try {//나중에 토큰에 넣을거 더 넣기
            return Jwts.builder()
                .claim("memberId", member.id().toString())
                .claim("type", "ACCESS")
                .setIssuedAt(dateNow)
                .setExpiration(dateExp)
                .signWith(getSigningKey())
                .compact();
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }

    }

    private RefreshToken generateRefreshToken(String accessToken, Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expirationTime = now.plus(jwtProperties.refreshToken(), ChronoUnit.MILLIS);

        Date dateNow = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        Date dateExp = Date.from(expirationTime.atZone(ZoneId.systemDefault()).toInstant());

        try {
            String token = Jwts.builder()
                .claim("memberId", memberId.toString())
                .claim("type", "REFRESH")
                .setIssuedAt(dateNow)
                .setExpiration(dateExp)
                .signWith(getSigningKey())
                .compact();
            return new RefreshToken(memberId, accessToken, token, now, expirationTime);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException();
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
