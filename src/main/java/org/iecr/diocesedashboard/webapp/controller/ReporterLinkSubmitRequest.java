package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.Valid;

import java.util.List;

/**
 * Request body for submitting a service instance via a reporter link token.
 * The church is taken from the authenticated reporter's profile;
 * the service template and service date are resolved from the link token.
 */
public record ReporterLinkSubmitRequest(
List<Long> celebrantIds,
@Valid List<ServiceInstanceRequest.ResponseEntry> responses) {
}
