package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for a client-side JavaScript error report.
 *
 * @param message the error message (required)
 * @param stack the stack trace or source location, if available
 * @param url the page URL where the error occurred
 * @param userAgent the browser user-agent string
 */
public record ClientErrorRequest(
@NotBlank @Size(max = 500) String message,
@Size(max = 5000) String stack,
@Size(max = 500) String url,
@Size(max = 500) String userAgent) {
}
