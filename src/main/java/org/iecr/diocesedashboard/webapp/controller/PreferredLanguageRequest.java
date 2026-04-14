package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Request body for updating the authenticated user's preferred language. */
public record PreferredLanguageRequest(
    @NotBlank @Pattern(regexp = "en|es", message = "language must be one of: en, es") String language) {
}
