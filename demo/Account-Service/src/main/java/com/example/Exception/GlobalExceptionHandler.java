package com.example.Exception;

import com.example.Model.Dto.Response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Value("${spring.application.bad-request:400}")
    private String badRequestCode;

    @Value("${spring.application.conflict:409}")
    private String conflictCode;

    @Value("${spring.application.not_found:404}")
    private String notFoundCode;

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");

        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .responseCode(badRequestCode)
                        .responseMessage(message)
                        .build()
        );
    }

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ApiResponse> handleGlobalException(GlobalException ex) {
        String errorCode = ex.getErrorCode() == null || ex.getErrorCode().isBlank()
                ? badRequestCode
                : ex.getErrorCode();

        HttpStatus status = resolveHttpStatus(errorCode);
        return ResponseEntity.status(status).body(
                ApiResponse.builder()
                        .responseCode(errorCode)
                        .responseMessage(ex.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .responseCode(badRequestCode)
                        .responseMessage(ex.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse> handleIllegalStateException(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiResponse.builder()
                        .responseCode(conflictCode)
                        .responseMessage(ex.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ApiResponse> handleEntityNotFoundException(jakarta.persistence.EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.builder()
                        .responseCode(notFoundCode)
                        .responseMessage(ex.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.builder()
                        .responseCode(GlobalError.INTERNAL_SERVER_ERROR)
                        .responseMessage(ex.getMessage() == null ? "Internal server error" : ex.getMessage())
                        .build()
        );
    }

    private HttpStatus resolveHttpStatus(String errorCode) {
        if (notFoundCode.equals(errorCode) || GlobalError.NOT_FOUND.equals(errorCode)) {
            return HttpStatus.NOT_FOUND;
        }
        if (conflictCode.equals(errorCode) || GlobalError.CONFLICT.equals(errorCode)) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
