package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Request body for reordering all template items (info items and section headers) together. */
public record TemplateItemReorderRequest(
@NotNull Long templateId,
@NotNull @NotEmpty List<@NotNull @Valid TemplateItemRef> items) {
}
