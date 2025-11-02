package org.ku.voicemap.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {

        ErrorResponse errorResponse = new ErrorResponse(e);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse);
    }

    @ExceptionHandler(VoiceMapException.class)
    public ResponseEntity<ErrorResponse> handleOotdException(VoiceMapException e) {

        HttpStatus status = determineHttpStatus(e);

        ErrorResponse response = new ErrorResponse(e);

        return ResponseEntity
            .status(status)
            .body(response);
    }


    private HttpStatus determineHttpStatus(VoiceMapException e) {

        ErrorCode errorCode = e.getErrorCode();

        return switch (errorCode) {
            case AUTHENTICATION_FAILED -> HttpStatus.UNAUTHORIZED;
            case MEMBER_EXIST_REGISTER, MEMBER_NOT_FOUND -> HttpStatus.CONFLICT;
        };
    }


}
