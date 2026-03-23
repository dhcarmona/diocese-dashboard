package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.iecr.diocesedashboard.domain.objects.UserRole;

/**
 * Request body for creating or updating a dashboard user account.
 *
 * <p>Password is optional on update; if omitted, the existing password is preserved.
 * For REPORTER role, {@code churchName} is required.
 */
public record UserRequest(
@NotBlank String username,
String password,
@NotNull UserRole role,
String churchName) {
}
