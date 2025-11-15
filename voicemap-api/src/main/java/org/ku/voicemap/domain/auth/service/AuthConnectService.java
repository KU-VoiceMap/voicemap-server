package org.ku.voicemap.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.auth.dto.ExternalMember;
import org.ku.voicemap.domain.auth.dto.TokenResponse;
import org.ku.voicemap.domain.auth.entity.AuthClient;
import org.ku.voicemap.domain.auth.entity.AuthClientRepository;
import org.ku.voicemap.domain.auth.entity.Token;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthConnectService {

    private final TokenProvider tokenProvider;
    private final AuthClientRepository authClientRepository;

    @Transactional
    public TokenResponse connect(ExternalMember externalMember, String memberNumber) {
        AuthClient authClient = authClientRepository.findByProviderAndPrincipal(externalMember.provider(), externalMember.principal())
            .orElseThrow(() -> new IllegalArgumentException("인증 정보가 존재하지 않습니다."));
        authClient.connect(memberNumber);
        authClientRepository.save(authClient);
        Token token = tokenProvider.generateToken(authClient);
        return new TokenResponse(token.getAccessToken(), token.getRefreshToken());
    }
}
