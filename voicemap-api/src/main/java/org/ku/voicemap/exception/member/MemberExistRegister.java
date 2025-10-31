package org.ku.voicemap.exception.member;

import org.ku.voicemap.domain.oauth.dto.RegisterDto;
import org.ku.voicemap.exception.ErrorCode;

public class MemberExistRegister extends MemberException {

    public MemberExistRegister(RegisterDto registerDto) {
        super(ErrorCode.MEMBER_EXIST_REGISTER);
        this.addDetail("provider", registerDto.provider());
        this.addDetail("providerId", registerDto.providerId());
        this.addDetail("email", registerDto.email());
    }
}
