package org.ku.voicemap.domain.oauth.service;

import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.jwt.JwtService;
import org.ku.voicemap.domain.jwt.Token;
import org.ku.voicemap.domain.jwt.TokenRepository;
import org.ku.voicemap.domain.jwt.TokenInfo;
import org.ku.voicemap.domain.member.entity.MemberDto;
import org.ku.voicemap.domain.member.model.Provider;
import org.ku.voicemap.domain.member.service.MemberServiceInter;
import org.ku.voicemap.domain.oauth.dto.AuthResponse;
import org.ku.voicemap.domain.oauth.dto.RegisterDto;
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

    public AuthResponse register(Provider provider, String idToken) {

        RegisterDto registerInfo = verifyIdToken(provider, idToken);

        MemberDto memberDto = memberService.createMember(registerInfo);

        TokenInfo tokenInfo = jwtService.generateToken(memberDto);

        //여기에 클라이언트에서 쓸 llm 토큰도 발급하는 코드 추가 예정

        return new AuthResponse(tokenInfo.accessToken(), tokenInfo.refreshToken());
    }

    @Transactional
    public AuthResponse login(Provider provider, String idToken) {

        RegisterDto registerInfo = verifyIdToken(provider, idToken);

        MemberDto memberDto = memberService.findMember(registerInfo);
        TokenInfo tokenInfo = jwtService.generateToken(memberDto);

        //여기에 클라이언트에서 쓸 llm 토큰도 발급하는 코드 추가 예정

        return new AuthResponse(tokenInfo.accessToken(), tokenInfo.refreshToken());
    }

    @Transactional
    public void logout(String clientRefreshToken) {
        Token token = tokenRepository.findByRefreshToken(clientRefreshToken)
            .orElseThrow(IllegalArgumentException::new);
        token.updatePossible();
    }

    @Transactional
    public AuthResponse rotateAccessToken(String clientRefreshToken) {
        TokenInfo tokenInfo = jwtService.rotateAccessToken(clientRefreshToken);
        return new AuthResponse(tokenInfo.accessToken(), tokenInfo.refreshToken());
    }

    @Transactional
    public AuthResponse rotateRefreshToken(String clientRefreshToken) {
        TokenInfo tokenInfo = jwtService.rotateRefreshToken(clientRefreshToken);
        return new AuthResponse(tokenInfo.accessToken(), tokenInfo.refreshToken());
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
