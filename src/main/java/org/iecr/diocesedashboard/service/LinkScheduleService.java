package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.LinkSchedule;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.repositories.LinkScheduleRepository;
import org.iecr.diocesedashboard.webapp.controller.LinkScheduleRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Service for managing {@link LinkSchedule} entities and executing them. */
@Service
public class LinkScheduleService {

  private static final Logger logger = LoggerFactory.getLogger(LinkScheduleService.class);

  private final LinkScheduleRepository repository;
  private final ServiceTemplateService serviceTemplateService;
  private final ChurchService churchService;
  private final ReporterLinkService reporterLinkService;

  @Autowired
  public LinkScheduleService(LinkScheduleRepository repository,
      ServiceTemplateService serviceTemplateService,
      ChurchService churchService,
      ReporterLinkService reporterLinkService) {
    this.repository = repository;
    this.serviceTemplateService = serviceTemplateService;
    this.churchService = churchService;
    this.reporterLinkService = reporterLinkService;
  }

  /** Returns all persisted link schedules. */
  public List<LinkSchedule> findAll() {
    return repository.findAll();
  }

  /**
   * Returns the link schedule with the given ID, or empty if not found.
   *
   * @param id the schedule ID
   * @return an {@link Optional} containing the schedule if found
   */
  public Optional<LinkSchedule> findById(Long id) {
    return repository.findById(id);
  }

  /**
   * Creates and persists a new link schedule from the given request.
   *
   * @param request the schedule creation request
   * @return the saved {@link LinkSchedule}
   */
  @Transactional
  public LinkSchedule create(LinkScheduleRequest request) {
    LinkSchedule schedule = new LinkSchedule();
    schedule.setCreatedAt(Instant.now());
    applyRequest(schedule, request);
    return repository.save(schedule);
  }

  /**
   * Updates an existing link schedule identified by {@code id} with the given request.
   *
   * @param id      the ID of the schedule to update
   * @param request the update request
   * @return the updated and saved {@link LinkSchedule}
   * @throws ResponseStatusException 404 if the schedule does not exist
   */
  @Transactional
  public LinkSchedule update(Long id, LinkScheduleRequest request) {
    LinkSchedule schedule = repository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Link schedule not found: " + id));
    applyRequest(schedule, request);
    return repository.save(schedule);
  }

  /**
   * Deletes the link schedule with the given ID.
   *
   * @param id the ID of the schedule to delete
   * @throws ResponseStatusException 404 if the schedule does not exist
   */
  @Transactional
  public void delete(Long id) {
    if (!repository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "Link schedule not found: " + id);
    }
    repository.deleteById(id);
  }

  /**
   * Executes a link schedule: resolves its churches, calls bulk link creation (including
   * WhatsApp notifications), then records today as the last triggered date.
   *
   * @param schedule the schedule to execute
   * @param baseUrl  the server base URL used to build the link URLs sent to reporters
   */
  @Transactional
  public void executeSchedule(LinkSchedule schedule, String baseUrl) {
    List<Church> churches = schedule.getChurchNames().stream()
        .map(name -> churchService.findById(name).orElse(null))
        .filter(Objects::nonNull)
        .toList();

    if (churches.isEmpty()) {
      logger.warn("Schedule {} has no resolvable churches, skipping.", schedule.getId());
      return;
    }

    LocalDate today = LocalDate.now();
    logger.info("Executing schedule {} for {} church(es) on {}",
        schedule.getId(), churches.size(), today);

    reporterLinkService.createLinksForChurches(
        churches, schedule.getServiceTemplate(), today, baseUrl);

    schedule.setLastTriggeredDate(today);
    repository.save(schedule);
  }

  private void applyRequest(LinkSchedule schedule, LinkScheduleRequest request) {
    ServiceTemplate template = serviceTemplateService.findById(request.serviceTemplateId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Service template not found: " + request.serviceTemplateId()));
    schedule.setServiceTemplate(template);
    schedule.setSendHour(request.sendHour());
    schedule.setDaysOfWeek(new HashSet<>(request.daysOfWeek()));
    schedule.setChurchNames(new HashSet<>(request.churchNames()));
  }
}
