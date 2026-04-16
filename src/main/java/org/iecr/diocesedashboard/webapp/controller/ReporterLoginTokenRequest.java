package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.constraints.NotBlank;

/** Request body for redeeming a magic login token. */
public record ReporterLoginTokenRequest(@NotBlank String token) {
}
