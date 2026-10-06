package com.sliit.helpdesk.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sliit.helpdesk.auth.service.AdminAccountProtectedException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Returns a consistent JSON body for every REST API failure instead of a raw stack trace.
 */
@ControllerAdvice(annotations = RestController.class)
@ResponseBody
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AdminAccountProtectedException.class)
    public ResponseEntity<Map<String, Object>> handleProtectedAdmin(
            AdminAccountProtectedException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        String message = safeMessage(ex.getMessage(), "Bad request");
        return error(statusForIllegalArgument(message), message, request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        log.warn("Illegal state on {}: {}", request.getRequestURI(), ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, safeMessage(ex.getMessage(), "Invalid request state"), request);
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<Map<String, Object>> handleTransactionSystem(
            TransactionSystemException ex,
            HttpServletRequest request
    ) {
        log.error("Transaction error on {}", request.getRequestURI(), ex);
        Throwable root = ex.getRootCause();
        if (root instanceof ConstraintViolationException cve) {
            String message = cve.getConstraintViolations().stream()
                    .findFirst()
                    .map(v -> v.getMessage() == null ? "Validation failed" : v.getMessage())
                    .orElse("Validation failed");
            return error(HttpStatus.BAD_REQUEST, message, request);
        }
        String message = root != null && root.getMessage() != null && !root.getMessage().isBlank()
                ? root.getMessage()
                : "A database transaction error occurred.";
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.error("Data integrity violation on {}", request.getRequestURI(), ex);
        Throwable root = ex.getRootCause();
        String message = root != null && root.getMessage() != null && !root.getMessage().isBlank()
                ? root.getMessage()
                : "A data integrity violation occurred.";
        return error(HttpStatus.CONFLICT, message, request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(
            DataAccessException ex,
            HttpServletRequest request
    ) {
        log.error("Database access error on {}", request.getRequestURI(), ex);
        Throwable root = ex.getRootCause();
        String message = root != null && root.getMessage() != null && !root.getMessage().isBlank()
                ? root.getMessage()
                : safeMessage(ex.getMessage(), "Database error occurred.");
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Database error: " + message, request);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipart(
            MultipartException ex,
            HttpServletRequest request
    ) {
        log.warn("Multipart error on {}: {}", request.getRequestURI(), ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "File upload failed: " + safeMessage(ex.getMessage(), "Invalid upload"), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        return validationError(ex.getBindingResult(), request);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBind(BindException ex, HttpServletRequest request) {
        return validationError(ex.getBindingResult(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage() == null ? "Validation failed" : violation.getMessage())
                .orElse("Validation failed");
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, "Request body is missing or invalid", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, "Missing required parameter: " + ex.getParameterName(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, "Invalid value for parameter: " + ex.getName(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleUploadTooLarge(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file is too large", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMissingResource(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.NOT_FOUND, "Resource not found", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.FORBIDDEN, "Forbidden", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        return error(HttpStatus.UNAUTHORIZED, "Unauthorized", request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return error(status, safeMessage(ex.getReason(), status.getReasonPhrase()), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled API error on {}", request.getRequestURI(), ex);
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String msg = root.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = ex.getMessage();
        }
        if (msg == null || msg.isBlank()) {
            msg = "An unexpected error occurred. Please verify your request details.";
        }
        return error(HttpStatus.INTERNAL_SERVER_ERROR, msg, request);
    }

    private static HttpStatus statusForIllegalArgument(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("not found")) {
            return HttpStatus.NOT_FOUND;
        }
        if (lower.contains("unauthorized")
                || lower.contains("authentication required")
                || lower.contains("invalid email")
                || lower.contains("invalid credentials")) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (lower.contains("already exists")) {
            return HttpStatus.CONFLICT;
        }
        if (lower.contains("disabled")
                || lower.contains("cannot")
                || lower.contains("only staff")) {
            return HttpStatus.FORBIDDEN;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private static ResponseEntity<Map<String, Object>> validationError(BindingResult result, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : result.getFieldErrors()) {
            String message = fieldError.getDefaultMessage() == null ? "Invalid value" : fieldError.getDefaultMessage();
            fields.putIfAbsent(fieldError.getField(), message);
        }
        String message = fields.isEmpty() ? "Validation failed" : fields.values().iterator().next();
        ResponseEntity<Map<String, Object>> response = error(HttpStatus.BAD_REQUEST, message, request);
        if (!fields.isEmpty() && response.getBody() != null) {
            response.getBody().put("fields", fields);
        }
        return response;
    }

    private static ResponseEntity<Map<String, Object>> error(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        body.put("status", status.value());
        body.put("path", request.getRequestURI());
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(body);
    }

    private static String safeMessage(String message, String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }
}
