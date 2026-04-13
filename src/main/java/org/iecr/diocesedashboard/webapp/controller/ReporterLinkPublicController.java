package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.Valid;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ReporterLink;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.service.CelebrantService;
import org.iecr.diocesedashboard.service.ReporterLinkService;
import org.iecr.diocesedashboard.service.ServiceSubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * Publicly accessible API endpoints for reporter links.
 * The token itself serves as the authorization credential; no session is required.
 */
@RestController
@RequestMapping("/api/reporter-links/public")
public class ReporterLinkPublicController {

  private final ReporterLinkService reporterLinkService;
  private final CelebrantService celebrantService;
  private final ServiceSubmissionService serviceSubmissionService;

  @Autowired
  public ReporterLinkPublicController(ReporterLinkService reporterLinkService,
      CelebrantService celebrantService, ServiceSubmissionService serviceSubmissionService) {
    this.reporterLinkService = reporterLinkService;
    this.celebrantService = celebrantService;
    this.serviceSubmissionService = serviceSubmissionService;
  }

  /**
   * Returns the reporter link data for the given token, including all information
   * needed to render the report submission form, without requiring authentication.
   *
   * @param token the reporter link token
   * @return 200 with the public link response, or 404 if the token is unknown
   */
  @GetMapping("/{token}")
  public ResponseEntity<ReporterLinkPublicResponse> getByToken(@PathVariable String token) {
    ReporterLink link = reporterLinkService.findByToken(token)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Reporter link not found"));
    List<ServiceInfoItemSummary> infoItems = link.getServiceTemplate().getServiceInfoItems()
        .stream().map(ServiceInfoItemSummary::from).toList();
    List<CelebrantSummary> celebrants = celebrantService.findAll()
        .stream().map(CelebrantSummary::from).toList();
    return ResponseEntity.ok(ReporterLinkPublicResponse.from(link, infoItems, celebrants));
  }

  /**
   * Submits a new service instance via the reporter link identified by the given token.
   * No authentication is required; the token itself is the authorization credential.
   * The link must be active (its activeDate must be today or in the past), and it is
   * revoked upon successful submission.
   *
   * @param token   the reporter link token
   * @param request the submission data (celebrants, date, responses)
   * @return 201 with the created service instance identifier, 404 if the token is unknown,
   *         or 409 if the link is not yet active
   */
  @PostMapping("/{token}/submit")
  public ResponseEntity<ReporterLinkSubmissionResponse> submit(@PathVariable String token,
      @RequestBody @Valid ReporterLinkSubmitRequest request) {
    ReporterLink link = reporterLinkService.findByToken(token)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Reporter link not found"));
    DashboardUser reporter = link.getReporter();
    if (!reporter.isEnabled()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "Reporter account is disabled");
    }
    if (!reporter.isAssignedToChurch(link.getChurch())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "Reporter is not assigned to this church");
    }
    if (link.getActiveDate().isAfter(LocalDate.now())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "This reporter link is not yet active until " + link.getActiveDate());
    }
    ServiceInstanceRequest instanceRequest = new ServiceInstanceRequest(
        link.getChurch().getName(),
        request.celebrantIds(),
        request.serviceDate(),
        request.responses());
    ServiceInstance created = serviceSubmissionService.submit(
        link.getServiceTemplate().getId(), instanceRequest, link.getReporter());
    reporterLinkService.deleteByToken(token);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ReporterLinkSubmissionResponse.from(created));
  }
}
