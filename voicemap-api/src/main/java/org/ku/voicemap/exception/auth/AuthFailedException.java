package org.ku.voicemap.exception.auth;

import org.ku.voicemap.exception.ErrorCode;

public class AuthFailedException extends AuthException {

    public AuthFailedException(String idToken) {
        super(ErrorCode.AUTHENTICATION_FAILED);
    }
}
