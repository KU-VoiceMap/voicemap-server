package org.ku.voicemap.exception.member;

import org.ku.voicemap.domain.oauth.dto.RegisterDto;
import org.ku.voicemap.exception.ErrorCode;

public class MemberNotFoundException extends MemberException{

  public MemberNotFoundException(RegisterDto registerDto) {
    super(ErrorCode.MEMBER_NOT_FOUND);
    this.addDetail("provider",registerDto.provider());
    this.addDetail("providerId",registerDto.providerId());
    this.addDetail("email",registerDto.email());
  }
}
