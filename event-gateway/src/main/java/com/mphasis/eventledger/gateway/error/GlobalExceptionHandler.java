package com.mphasis.eventledger.gateway.error;

import com.mphasis.eventledger.gateway.client.AccountClientException;
import com.mphasis.eventledger.gateway.service.EventNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.stream.Collectors;

import static com.mphasis.eventledger.gateway.error.TraceSupport.responseTraceId;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException ex, NativeWebRequest request) {
        var message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .sorted()
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> malformedJson(HttpMessageNotReadableException ex, NativeWebRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "request body is invalid: " + ex.getMostSpecificCause().getMessage(), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> missingParameter(MissingServletRequestParameterException ex, NativeWebRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), request);
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> eventNotFound(EventNotFoundException ex, NativeWebRequest request) {
        return error(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(AccountClientException.class)
    public ResponseEntity<ApiErrorResponse> accountServiceUnavailable(AccountClientException ex, NativeWebRequest request) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "ACCOUNT_SERVICE_UNAVAILABLE", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> unexpected(Exception ex, NativeWebRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex.getMessage(), request);
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            NativeWebRequest request
    ) {
        var traceId = responseTraceId(request.getHeader("X-Trace-ID"));
        return ResponseEntity.status(status)
                .header("X-Trace-ID", traceId)
                .body(ApiErrorResponse.of(code, message, traceId));
    }

    private String formatFieldError(FieldError error) {
        return "%s %s".formatted(error.getField(), error.getDefaultMessage());
    }
}
