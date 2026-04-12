package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.Valid;
import org.iecr.diocesedashboard.domain.objects.LinkSchedule;
import org.iecr.diocesedashboard.service.LinkScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing reporter link schedules. ADMIN only.
 *
 * <p>Schedules are recurring configurations that automatically create and send reporter
 * links on specific days of the week at a given hour in Costa Rica time.
 */
@RestController
@RequestMapping("/api/link-schedules")
public class LinkScheduleController {

  private final LinkScheduleService linkScheduleService;

  @Autowired
  public LinkScheduleController(LinkScheduleService linkScheduleService) {
    this.linkScheduleService = linkScheduleService;
  }

  /**
   * Returns all link schedules.
   *
   * @return list of all link schedules
   */
  @GetMapping
  public List<LinkScheduleResponse> getAll() {
    return linkScheduleService.findAll().stream()
        .map(LinkScheduleResponse::from)
        .toList();
  }

  /**
   * Creates a new link schedule.
   *
   * @param request the schedule creation request
   * @return 201 with the created schedule
   */
  @PostMapping
  public ResponseEntity<LinkScheduleResponse> create(
      @RequestBody @Valid LinkScheduleRequest request) {
    LinkSchedule created = linkScheduleService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(LinkScheduleResponse.from(created));
  }

  /**
   * Updates an existing link schedule.
   *
   * @param id      the ID of the schedule to update
   * @param request the update request
   * @return 200 with the updated schedule, or 404 if not found
   */
  @PutMapping("/{id}")
  public ResponseEntity<LinkScheduleResponse> update(@PathVariable Long id,
      @RequestBody @Valid LinkScheduleRequest request) {
    LinkSchedule updated = linkScheduleService.update(id, request);
    return ResponseEntity.ok(LinkScheduleResponse.from(updated));
  }

  /**
   * Deletes the link schedule with the given ID.
   *
   * @param id the ID of the schedule to delete
   * @return 204 on success, or 404 if not found
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    linkScheduleService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
