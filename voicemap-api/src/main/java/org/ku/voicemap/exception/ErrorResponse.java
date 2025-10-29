package org.ku.voicemap.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ErrorResponse {

  // TODO: 요구사항에 timestamp, errorcode 없음 의논 필요
  private final String exceptionName;
  private final String message;
  private final Map<String, Object> details;

  public ErrorResponse(VoiceMapException e) {
    this(e.getErrorCode().name(), e.getMessage(), e.getDetails());
  }

  public ErrorResponse(Exception e) {
    this(e.getClass().getSimpleName(), e.getMessage(), new HashMap<>());
  }
}

