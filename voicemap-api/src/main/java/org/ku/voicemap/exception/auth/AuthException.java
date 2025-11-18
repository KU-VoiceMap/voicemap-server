package org.ku.voicemap.exception.auth;

import org.ku.voicemap.exception.ErrorCode;
import org.ku.voicemap.exception.VoiceMapException;

public class AuthException extends VoiceMapException {

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }
}
