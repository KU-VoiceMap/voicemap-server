package org.ku.voicemap.domain.auth.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.auth.AuthProvider;
import org.ku.voicemap.domain.auth.dto.TokenResponse;
import org.ku.voicemap.domain.auth.entity.AuthClient;
import org.ku.voicemap.domain.auth.entity.AuthClientRepository;
import org.ku.voicemap.domain.auth.entity.Token;
import org.ku.voicemap.domain.auth.entity.TokenRepository;
import org.ku.voicemap.domain.auth.verify.TokenVerify;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TokenVerify tokenVerify;
    private final TokenProvider tokenProvider;
    private final TokenRepository tokenRepository;
    private final AuthClientRepository authClientRepository;

    @Transactional
    public TokenResponse login(String email, AuthProvider provider, String principal) {
        AuthClient authClient = authClientRepository.findByProviderAndPrincipal(provider, principal)
            .orElseGet(() -> authClientRepository.save(new AuthClient(email, provider, principal)));

        if (!authClient.isConnected()) {
            throw new AuthClientNotConnectedException();
        }
        Token token = tokenProvider.generateToken(authClient.getMemberNumber());
        return new TokenResponse(token.getAccessToken(), token.getRefreshToken());
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
        String newAccessToken = tokenProvider.generateAccessToken(memberNumber);
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
        String memberNumber = token.getAuthClient().getMemberNumber();
        Token newToken = tokenProvider.generateToken(memberNumber);
        tokenRepository.saveAll(List.of(token, newToken));
        return new TokenResponse(newToken.getAccessToken(), newToken.getRefreshToken());
    }
}
