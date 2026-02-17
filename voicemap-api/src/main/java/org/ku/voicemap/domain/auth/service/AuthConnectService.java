package org.ku.voicemap.domain.auth.service;

import java.time.LocalDateTime;
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
public class AuthConnectService {

    private final TokenProvider tokenProvider;
    private final TokenRepository tokenRepository;
    private final AuthClientRepository authClientRepository;

    @Transactional
    public TokenResponse connect(ExternalMember externalMember, String memberNumber, LocalDateTime now) {
        AuthClient authClient = authClientRepository.findByProviderAndPrincipal(externalMember.provider(), externalMember.principal())
            .orElseGet(() -> authClientRepository.save(new AuthClient(externalMember.email(), externalMember.provider(), externalMember.principal())));
        authClient.connect(memberNumber);
        authClientRepository.save(authClient);
        TokenPair tokenPair = tokenProvider.generateTokenPair(authClient.getMemberNumber(), now);
        tokenRepository.save(new Token(authClient, tokenPair.accessToken(), tokenPair.refreshToken(), now, tokenPair.expireAt()));
        return new TokenResponse(tokenPair.accessToken(), tokenPair.refreshToken());
    }
}
