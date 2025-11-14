package org.ku.voicemap.domain.oauth.service;

import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.ephemeralToken.dto.CreateEphemeralRequest;
import org.ku.voicemap.domain.ephemeralToken.dto.EphemeralTokenResponse;
import org.ku.voicemap.domain.ephemeralToken.service.EphemeralService;
import org.ku.voicemap.domain.jwt.JwtService;
import org.ku.voicemap.domain.jwt.Token;
import org.ku.voicemap.domain.jwt.TokenInfo;
import org.ku.voicemap.domain.jwt.TokenRepository;
import org.ku.voicemap.domain.member.entity.MemberDto;
import org.ku.voicemap.domain.member.model.Provider;
import org.ku.voicemap.domain.member.service.MemberServiceInter;
import org.ku.voicemap.domain.oauth.dto.AuthResponse;
import org.ku.voicemap.domain.oauth.dto.RegisterDto;
import org.ku.voicemap.domain.oauth.dto.RotateResponse;
import org.ku.voicemap.domain.oauth.verify.TokenVerify;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberServiceInter memberService;
    private final TokenVerify tokenVerify;
    private final JwtService jwtService;
    private final TokenRepository tokenRepository;
    private final EphemeralService ephemeralService;

    public AuthResponse register(Provider provider, String idToken) {

        RegisterDto registerInfo = verifyIdToken(provider, idToken);

        MemberDto memberDto = memberService.createMember(registerInfo);

        TokenInfo tokenInfo = jwtService.generateToken(memberDto);

        EphemeralTokenResponse ephemeralTokenResponse = ephemeralService.createEphemeralToken(memberDto.id(),
            new CreateEphemeralRequest(10, 5, 2));

        return new AuthResponse(tokenInfo.accessToken(), tokenInfo.refreshToken(),ephemeralTokenResponse);
    }

    @Transactional
    public AuthResponse login(Provider provider, String idToken) {

        RegisterDto registerInfo = verifyIdToken(provider, idToken);

        MemberDto memberDto = memberService.findMember(registerInfo);
        TokenInfo tokenInfo = jwtService.generateToken(memberDto);

        EphemeralTokenResponse ephemeralTokenResponse = ephemeralService.createEphemeralToken(memberDto.id(),
            new CreateEphemeralRequest(10, 5, 2));

        return new AuthResponse(tokenInfo.accessToken(), tokenInfo.refreshToken(),ephemeralTokenResponse);
    }

    @Transactional
    public void logout(String clientRefreshToken) {
        Token token = tokenRepository.findByRefreshToken(clientRefreshToken)
            .orElseThrow(IllegalArgumentException::new);
        token.updatePossible();
    }

    @Transactional
    public RotateResponse rotateAccessToken(String clientRefreshToken) {
        TokenInfo tokenInfo = jwtService.rotateAccessToken(clientRefreshToken);
        return new RotateResponse(tokenInfo.accessToken(), tokenInfo.refreshToken());
    }

    @Transactional
    public RotateResponse rotateRefreshToken(String clientRefreshToken) {
        TokenInfo tokenInfo = jwtService.rotateRefreshToken(clientRefreshToken);
        return new RotateResponse(tokenInfo.accessToken(), tokenInfo.refreshToken());
    }


    //Provider마다 토큰 다르게 검증
    private RegisterDto verifyIdToken(Provider provider, String idToken) {

        RegisterDto registerInfo = null;

        if (provider == Provider.GOOGLE) {
            registerInfo = tokenVerify.toGoogle(idToken);
        }

        return registerInfo;
    }
}
