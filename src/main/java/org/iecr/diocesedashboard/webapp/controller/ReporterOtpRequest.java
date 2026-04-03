package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.constraints.NotBlank;

/** Request body for sending a reporter OTP to a user's registered phone number. */
public record ReporterOtpRequest(@NotBlank String username) {
}
