package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.constraints.NotNull;

/** Reference to a single template item (info item or section header) for reorder operations. */
public record TemplateItemRef(
@NotNull Long id,
@NotNull TemplateItemKind kind) {
}
