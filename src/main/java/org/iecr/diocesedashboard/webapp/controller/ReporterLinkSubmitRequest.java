package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.util.List;

/**
 * Request body for submitting a service instance via a reporter link token.
 * The church is taken from the authenticated reporter's profile;
 * the service template is resolved from the link token.
 */
public record ReporterLinkSubmitRequest(
List<Long> celebrantIds,
@NotNull @PastOrPresent LocalDate serviceDate,
@Valid List<ServiceInstanceRequest.ResponseEntry> responses) {
}
