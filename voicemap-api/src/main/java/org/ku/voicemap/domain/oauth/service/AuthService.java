package org.ku.voicemap.domain.oauth.service;

import lombok.RequiredArgsConstructor;
import org.ku.voicemap.domain.member.entity.MemberDto;
import org.ku.voicemap.domain.member.entity.Provider;
import org.ku.voicemap.domain.member.service.MemberServiceInter;
import org.ku.voicemap.domain.oauth.dto.AuthResponse;
import org.ku.voicemap.domain.oauth.dto.RegisterDto;
import org.ku.voicemap.domain.oauth.verify.TokenVerify;
import org.ku.voicemap.exception.member.MemberExistRegister;
import org.ku.voicemap.exception.member.MemberNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService implements AuthServiceInter {

  private final MemberServiceInter memberService;
  private final TokenVerify tokenVerify;

  @Transactional
  public AuthResponse register(Provider provider, String idToken) {

    RegisterDto registerInfo = convertToVerify(provider, idToken);

    if (memberService.checkRegister(registerInfo)) {
      throw new MemberExistRegister(registerInfo);
    }

    MemberDto memberDto = memberService.createMember(registerInfo);

    String appAccessToken = "임시임시-" + memberDto.id();//임시로
    return new AuthResponse(appAccessToken);
  }

  @Transactional(readOnly = true)
  public AuthResponse login(Provider provider, String idToken) {

    RegisterDto registerInfo = convertToVerify(provider, idToken);

    if (!memberService.checkRegister(registerInfo)) {
      throw new MemberNotFoundException(registerInfo);
    }
    MemberDto memberDto = memberService.findMember(registerInfo);

    String appAccessToken = "임시임시-" + memberDto.id();//임시로
    return new AuthResponse(appAccessToken);
  }

  //Provider마다 토큰 다르게 검증
  private RegisterDto convertToVerify(Provider provider, String idToken) {
    RegisterDto registerInfo = null;
    if (provider == Provider.GOOGLE) {
      registerInfo = tokenVerify.toGoogle(idToken);
    }
    return registerInfo;
  }
}