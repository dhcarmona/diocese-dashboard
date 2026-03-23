package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for creating a new reporter link.
 * Only ADMIN users may create links.
 */
public record ReporterLinkRequest(
@NotNull Long reporterId,
@NotNull Long serviceTemplateId) {
}
