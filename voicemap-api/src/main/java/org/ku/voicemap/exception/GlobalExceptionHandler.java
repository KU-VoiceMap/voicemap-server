package org.ku.voicemap.exception;

import lombok.extern.slf4j.Slf4j;
import org.ku.voicemap.domain.auth.service.InvalidTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {

        ErrorResponse errorResponse = new ErrorResponse(e);
        log.error("Unhandled exception occurred: {}", e.getMessage(), e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTokenException(InvalidTokenException e) {
        ErrorResponse errorResponse = new ErrorResponse(e);
        log.warn("Authentication failed: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(errorResponse);
    }

    @ExceptionHandler(VoiceMapException.class)
    public ResponseEntity<ErrorResponse> handleVoiceMapException(VoiceMapException e) {
        HttpStatus status = determineHttpStatus(e);
        ErrorResponse response = new ErrorResponse(e);

        return ResponseEntity.status(status).body(response);
    }

    private HttpStatus determineHttpStatus(VoiceMapException e) {

        ErrorCode errorCode = e.getErrorCode();

        return switch (errorCode) {
            case AUTHENTICATION_FAILED -> HttpStatus.UNAUTHORIZED;
            case MEMBER_EXIST_REGISTER, MEMBER_NOT_FOUND -> HttpStatus.CONFLICT;
        };
    }


}
