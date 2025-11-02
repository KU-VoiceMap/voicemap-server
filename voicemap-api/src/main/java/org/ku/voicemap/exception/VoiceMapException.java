package org.ku.voicemap.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class VoiceMapException extends RuntimeException {

    private final ErrorCode errorCode;

    public VoiceMapException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public VoiceMapException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public VoiceMapException(ErrorCode errorCode, Map<String, Object> details) {
        this(errorCode);
    }

    public VoiceMapException(ErrorCode errorCode, Map<String, Object> details, Throwable cause) {
        this(errorCode, cause);
    }

}
