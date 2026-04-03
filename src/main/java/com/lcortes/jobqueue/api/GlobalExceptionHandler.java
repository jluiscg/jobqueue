package com.lcortes.jobqueue.api;

import com.lcortes.jobqueue.api.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation errors from @Valid on DTOs (e.g., missing type, maxRetries > 10).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    return fieldName + ": " + errorMessage;
                })
                .toList();

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.UNPROCESSABLE_CONTENT.value(),
                "Validation Failed",
                "The request payload failed validation checks.",
                errors
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(response);
    }

    /**
     * Handles the IllegalArgumentException we throw in JobPriority.fromValue()
     * if a user submits an unknown priority level.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                List.of()
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Catch-all for any unhandled server errors so we never leak stack traces to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaughtException(Exception ex) {
        log.error("Unhandled exception caught in GlobalExceptionHandler", ex);

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred while processing the request.",
                List.of()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}