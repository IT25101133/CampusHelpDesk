package com.sliit.helpdesk.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage() == null ? "Bad request" : ex.getMessage();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String lower = message.toLowerCase();
        if (lower.contains("not found")) {
            status = HttpStatus.NOT_FOUND;
        } else if (lower.contains("cannot") || lower.contains("only staff")) {
            status = HttpStatus.FORBIDDEN;
        }
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
