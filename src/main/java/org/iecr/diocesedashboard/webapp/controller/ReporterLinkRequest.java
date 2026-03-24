package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for creating a new reporter link.
 * Only ADMIN users may create links.
 */
public record ReporterLinkRequest(
@NotNull Long reporterId,
@NotBlank String churchName,
@NotNull Long serviceTemplateId) {
}
