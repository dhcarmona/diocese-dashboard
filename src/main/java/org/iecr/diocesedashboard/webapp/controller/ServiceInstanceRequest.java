package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.util.List;

/**
 * Request body for submitting a new Service Instance against a Service Template.
 */
public record ServiceInstanceRequest(
@NotBlank String churchName,
List<Long> celebrantIds,
@NotNull @PastOrPresent LocalDate serviceDate,
@Valid List<ResponseEntry> responses) {

  /**
   * A single answer to one Service Info Item question.
   */
  public record ResponseEntry(
  @NotNull Long serviceInfoItemId,
  @NotBlank String responseValue) {
  }
}
