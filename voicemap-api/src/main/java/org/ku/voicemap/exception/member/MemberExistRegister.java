package org.ku.voicemap.exception.member;

import org.ku.voicemap.domain.oauth.dto.RegisterDto;
import org.ku.voicemap.exception.ErrorCode;

public class MemberExistRegister extends MemberException {

    public MemberExistRegister(RegisterDto registerDto) {
        super(ErrorCode.MEMBER_EXIST_REGISTER);
    }
}
