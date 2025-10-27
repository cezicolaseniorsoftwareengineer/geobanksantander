package com.santander.geobank.api.config;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.santander.geobank.application.dto.ErrorResponse;

/**
 * Global exception handler for GeoBank API.
 *
 * Provides centralized error handling with consistent response format.
 * Includes security-aware error sanitization and audit logging.
 * */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        String correlationId = getCorrelationId(request);

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());

        List<ErrorResponse.ValidationError> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.ValidationError(
                        error.getField(),
                        error.getRejectedValue(),
                        error.getDefaultMessage()))
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.validationError(
                "Dados invÃ¡lidos fornecidos",
                400,
                request.getDescription(false),
                validationErrors);

        logger.warn("Validation error - Correlation: {} - Errors: {}",
                correlationId, errors);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        String correlationId = getCorrelationId(request);

        ErrorResponse errorResponse = ErrorResponse.businessError(
                "ParÃ¢metros invÃ¡lidos: " + sanitizeMessage(ex.getMessage()),
                400,
                request.getDescription(false));

        logger.warn("Business rule violation - Correlation: {} - Message: {}",
                correlationId, ex.getMessage());

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {

        String correlationId = getCorrelationId(request);

        ErrorResponse errorResponse = ErrorResponse.businessError(
                "Estado invÃ¡lido: " + sanitizeMessage(ex.getMessage()),
                409,
                request.getDescription(false));

        logger.warn("State conflict - Correlation: {} - Message: {}",
                correlationId, ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex, WebRequest request) {

        String correlationId = getCorrelationId(request);

        ErrorResponse errorResponse = ErrorResponse.businessError(
                "Acesso negado: permissÃµes insuficientes",
                403,
                request.getDescription(false));

        logger.warn("Access denied - Correlation: {} - URI: {}",
                correlationId, request.getDescription(false));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {

        String correlationId = getCorrelationId(request);

        ErrorResponse errorResponse = ErrorResponse.serverError(
                "Erro interno do sistema",
                request.getDescription(false));

        logger.error("Unexpected error - Correlation: {} - Exception: {}",
                correlationId, ex.getClass().getSimpleName(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private String formatFieldError(FieldError fieldError) {
        return String.format("%s: %s", fieldError.getField(), fieldError.getDefaultMessage());
    }

    private String sanitizeMessage(String message) {
        // Remove sensitive information from error messages
        if (message == null)
            return "Erro nÃ£o especificado";

        // Remove potential sensitive patterns
        return message.replaceAll("\\b\\d{4,}\\b", "****") // Credit card numbers
                .replaceAll("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", "****@****.***") // Emails
                .substring(0, Math.min(message.length(), 200)); // Limit length
    }

    private String getCorrelationId(WebRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        return correlationId != null ? correlationId : java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}

