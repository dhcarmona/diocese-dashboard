package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Request body for reordering service info items within a template. */
public record ReorderRequest(
    @NotNull @NotEmpty List<Long> orderedIds) {
}
