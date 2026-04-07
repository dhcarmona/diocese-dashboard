package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Request body for bulk-creating reporter links.
 * One link is created per church, automatically resolved to the assigned reporter.
 * Only ADMIN users may call this endpoint.
 */
public record ReporterLinkBulkRequest(
@NotNull Long serviceTemplateId,
@NotNull LocalDate activeDate,
@NotEmpty List<String> churchNames) {
}
