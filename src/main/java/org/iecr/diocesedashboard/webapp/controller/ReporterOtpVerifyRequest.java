package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.constraints.NotBlank;

/** Request body for verifying a reporter OTP and creating a session. */
public record ReporterOtpVerifyRequest(@NotBlank String username, @NotBlank String code) {
}
