package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.iecr.diocesedashboard.domain.objects.UserRole;

import java.util.Set;

/**
 * Request body for creating or updating a dashboard user account.
 *
 * <p>For ADMIN role: {@code password} is required; {@code fullName} and {@code phoneNumber}
 * are ignored.
 * For REPORTER role: {@code password} must be omitted; {@code fullName}, {@code phoneNumber},
 * and {@code churchNames} are required.
 */
public record UserRequest(
@NotBlank String username,
String password,
@NotNull UserRole role,
Set<String> churchNames,
String fullName,
String phoneNumber) {
}
