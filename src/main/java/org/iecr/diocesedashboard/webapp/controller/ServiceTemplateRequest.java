package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.constraints.NotBlank;

/** Request body for creating or updating a ServiceTemplate. */
public record ServiceTemplateRequest(@NotBlank String serviceTemplateName) {
}
