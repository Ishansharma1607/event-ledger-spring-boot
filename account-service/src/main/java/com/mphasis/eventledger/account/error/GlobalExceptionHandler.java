package com.mphasis.eventledger.account.error;

import com.mphasis.eventledger.account.service.AccountIdMismatchException;
import com.mphasis.eventledger.account.service.AccountNotFoundException;
import com.mphasis.eventledger.account.service.CurrencyMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.stream.Collectors;

import static com.mphasis.eventledger.account.error.TraceSupport.responseTraceId;

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

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> accountNotFound(AccountNotFoundException ex, NativeWebRequest request) {
        return error(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(AccountIdMismatchException.class)
    public ResponseEntity<ApiErrorResponse> accountMismatch(AccountIdMismatchException ex, NativeWebRequest request) {
        return error(HttpStatus.BAD_REQUEST, "ACCOUNT_ID_MISMATCH", ex.getMessage(), request);
    }

    @ExceptionHandler(CurrencyMismatchException.class)
    public ResponseEntity<ApiErrorResponse> currencyMismatch(CurrencyMismatchException ex, NativeWebRequest request) {
        return error(HttpStatus.BAD_REQUEST, "CURRENCY_MISMATCH", ex.getMessage(), request);
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
