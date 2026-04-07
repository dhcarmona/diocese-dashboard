package org.iecr.diocesedashboard.webapp.controller;

import java.util.List;

/**
 * Response body for bulk reporter link creation.
 * Reports which links were created and which churches were skipped
 * (because no reporter user was assigned to them).
 */
public record ReporterLinkBulkResponse(
List<ReporterLinkResponse> created,
List<String> skippedChurches) {
}
