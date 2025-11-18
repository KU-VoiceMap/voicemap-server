package org.ku.voicemap.exception.member;

import org.ku.voicemap.exception.ErrorCode;
import org.ku.voicemap.exception.VoiceMapException;

public class MemberException extends VoiceMapException {

    public MemberException(ErrorCode errorCode) {
        super(errorCode);
    }
}
