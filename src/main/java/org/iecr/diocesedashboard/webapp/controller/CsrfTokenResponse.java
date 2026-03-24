package org.iecr.diocesedashboard.webapp.controller;

/**
 * Frontend-safe representation of the CSRF token needed for protected writes.
 *
 * @param headerName HTTP header name expected by Spring Security
 * @param token token value to send in that header
 */
public record CsrfTokenResponse(String headerName, String token) {
}
