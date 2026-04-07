package org.iecr.diocesedashboard.webapp.controller;

import java.util.List;

/** Request body for reordering service info items within a template. */
public record ReorderRequest(List<Long> orderedIds) {
}
