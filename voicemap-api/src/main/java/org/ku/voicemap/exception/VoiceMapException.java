package org.ku.voicemap.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class VoiceMapException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public VoiceMapException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    public VoiceMapException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    public VoiceMapException(ErrorCode errorCode, Map<String, Object> details) {
        this(errorCode);
        this.details.putAll(details);
    }

    public VoiceMapException(ErrorCode errorCode, Map<String, Object> details, Throwable cause) {
        this(errorCode, cause);
        this.details.putAll(details);
    }

    public void addDetail(String key, Object value) {
        this.details.put(key, value);
    }

}
