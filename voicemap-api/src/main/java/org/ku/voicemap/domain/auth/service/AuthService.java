package org.ku.voicemap.domain.auth.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.auth.dto.ExternalMember;
import org.ku.voicemap.domain.auth.dto.TokenResponse;
import org.ku.voicemap.domain.auth.entity.AuthClient;
import org.ku.voicemap.domain.auth.entity.AuthClientRepository;
import org.ku.voicemap.domain.auth.entity.Token;
import org.ku.voicemap.domain.auth.entity.TokenRepository;
import org.ku.voicemap.domain.auth.token.TokenPair;
import org.ku.voicemap.domain.auth.token.TokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TokenProvider tokenProvider;
    private final TokenRepository tokenRepository;
    private final AuthClientRepository authClientRepository;

    @Transactional
    public TokenResponse login(ExternalMember externalMember, LocalDateTime now) {
        AuthClient authClient = authClientRepository.findByProviderAndPrincipal(externalMember.provider(), externalMember.principal())
            .orElseThrow(AuthClientNotConnectedException::new);
        if (!authClient.isConnected()) {
            throw new AuthClientNotConnectedException();
        }
        TokenPair tokenPair = tokenProvider.generateTokenPair(authClient.getMemberNumber(), now);
        tokenRepository.save(new Token(authClient, tokenPair.accessToken(), tokenPair.refreshToken(), now, tokenPair.expireAt()));
        return new TokenResponse(tokenPair.accessToken(), tokenPair.refreshToken());
    }

    @Transactional
    public void logout(String clientRefreshToken) {
        Token token = tokenRepository.findByRefreshToken(clientRefreshToken)
            .orElseThrow(IllegalArgumentException::new);
        token.invalidate();
        tokenRepository.save(token);
    }

    @Transactional
    public TokenResponse rotateAccessToken(String refreshToken, LocalDateTime now) {
        Token token = tokenRepository.findByRefreshTokenWithAuthClient(refreshToken)
            .orElseThrow(IllegalArgumentException::new);
        if (!token.isValid(now)) {
            throw new InvalidTokenException();
        }
        String memberNumber = token.getAuthClient().getMemberNumber();
        String newAccessToken = tokenProvider.generateAccessToken(memberNumber, now);
        token.updateAccessToken(newAccessToken);
        tokenRepository.save(token);
        return new TokenResponse(newAccessToken, token.getRefreshToken());
    }

    @Transactional
    public TokenResponse rotateRefreshToken(String refreshToken, LocalDateTime now) {
        Token token = tokenRepository.findByRefreshTokenWithAuthClient(refreshToken)
            .orElseThrow(IllegalArgumentException::new);
        if (!token.isValid(now)) {
            throw new InvalidTokenException();
        }
        token.invalidate();
        AuthClient authClient = token.getAuthClient();
        TokenPair tokenPair = tokenProvider.generateTokenPair(authClient.getMemberNumber(), now);
        tokenRepository.saveAll(
            List.of(token, new Token(authClient, tokenPair.accessToken(), tokenPair.refreshToken(), now, tokenPair.expireAt()))
        );
        return new TokenResponse(tokenPair.accessToken(), tokenPair.refreshToken());
    }
}
