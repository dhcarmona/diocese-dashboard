package org.iecr.diocesedashboard.webapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Global exception handler for {@link ResponseStatusException} thrown by controllers or services.
 *
 * <p>Only errors signalled by throwing {@code ResponseStatusException} produce a standardized JSON
 * error body. Controllers that return {@link org.springframework.http.ResponseEntity} instances
 * directly (for example, {@code ResponseEntity.notFound().build()}) are not handled here and will
 * produce empty response bodies.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handles {@link ResponseStatusException} and returns a consistent JSON error body.
   *
   * @param ex the exception
   * @return a response containing the HTTP status code and reason message
   */
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
    return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
        "status", ex.getStatusCode().value(),
        "message", ex.getReason() != null ? ex.getReason() : ex.getMessage()
    ));
  }
}
