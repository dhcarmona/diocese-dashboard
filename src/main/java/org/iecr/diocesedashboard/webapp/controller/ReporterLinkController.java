package org.iecr.diocesedashboard.webapp.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ReporterLink;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.service.ChurchService;
import org.iecr.diocesedashboard.service.ReporterLinkService;
import org.iecr.diocesedashboard.service.ServiceSubmissionService;
import org.iecr.diocesedashboard.service.ServiceTemplateService;
import org.iecr.diocesedashboard.service.UserService;
import org.iecr.diocesedashboard.webapp.DashboardUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** REST controller for managing and using reporter short-URL links. */
@RestController
@RequestMapping("/api/reporter-links")
public class ReporterLinkController {

  private final ReporterLinkService reporterLinkService;
  private final UserService userService;
  private final ChurchService churchService;
  private final ServiceTemplateService serviceTemplateService;
  private final ServiceSubmissionService serviceSubmissionService;

  @Autowired
  public ReporterLinkController(ReporterLinkService reporterLinkService,
      UserService userService, ChurchService churchService,
      ServiceTemplateService serviceTemplateService,
      ServiceSubmissionService serviceSubmissionService) {
    this.reporterLinkService = reporterLinkService;
    this.userService = userService;
    this.churchService = churchService;
    this.serviceTemplateService = serviceTemplateService;
    this.serviceSubmissionService = serviceSubmissionService;
  }

  /**
   * Returns all reporter links. ADMIN only.
   *
   * @return list of all reporter links
   */
  @GetMapping
  public List<ReporterLinkResponse> getAll() {
    return reporterLinkService.findAll().stream()
        .map(ReporterLinkResponse::from)
        .toList();
  }

  /**
   * Creates a new reporter link for a specific REPORTER user and service template. ADMIN only.
   *
   * @param request the creation request containing reporterId, churchName, serviceTemplateId,
   *                and activeDate
   * @return 201 with the created reporter link details
   */
  @PostMapping
  public ResponseEntity<ReporterLinkResponse> create(
      @RequestBody @Valid ReporterLinkRequest request) {
    DashboardUser reporter = userService.findById(request.reporterId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "User not found: " + request.reporterId()));
    if (reporter.getRole() != UserRole.REPORTER) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Target user must have the REPORTER role");
    }
    Church church = churchService.findById(request.churchName())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Church not found: " + request.churchName()));
    if (!reporter.isAssignedToChurch(church)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Reporter is not assigned to church: " + request.churchName());
    }
    var template = serviceTemplateService.findById(request.serviceTemplateId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Service template not found: " + request.serviceTemplateId()));
    ReporterLink created = reporterLinkService.createLink(
        reporter, church, template, request.activeDate());
    return ResponseEntity.status(HttpStatus.CREATED).body(ReporterLinkResponse.from(created));
  }

  /**
   * Bulk-creates reporter links for the given service template, active date, and list of churches.
   * Each church is automatically matched to its assigned reporter. Churches with no reporter are
   * returned in the {@code skippedChurches} list. ADMIN only.
   *
   * @param request the bulk creation request
   * @return 201 with created links and any skipped churches
   */
  @PostMapping("/bulk")
  public ResponseEntity<ReporterLinkBulkResponse> createBulk(
      @RequestBody @Valid ReporterLinkBulkRequest request, HttpServletRequest httpRequest) {
    var template = serviceTemplateService.findById(request.serviceTemplateId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Service template not found: " + request.serviceTemplateId()));

    List<Church> churches = new ArrayList<>();
    for (String churchName : request.churchNames()) {
      Church church = churchService.findById(churchName)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "Church not found: " + churchName));
      churches.add(church);
    }

    String origin = httpRequest.getHeader("Origin");
    String baseUrl = origin != null && !origin.isBlank()
        ? origin
        : ServletUriComponentsBuilder.fromRequest(httpRequest)
        .replacePath("").replaceQuery(null).toUriString();
    ReporterLinkService.BulkCreateResult result =
        reporterLinkService.createLinksForChurches(churches, template, request.activeDate(),
            baseUrl);

    List<ReporterLinkResponse> createdResponses = result.created().stream()
        .map(ReporterLinkResponse::from)
        .toList();
    ReporterLinkBulkResponse body =
        new ReporterLinkBulkResponse(createdResponses, result.skippedChurches());
    return ResponseEntity.status(HttpStatus.CREATED).body(body);
  }

  /**
   * Returns the reporter link for the given token.
   * REPORTER users may only view links that belong to them.
   *
   * @param token the link token
   * @param auth  the current authentication
   * @return 200 with the link, or 404 if not found
   */
  @GetMapping("/{token}")
  public ResponseEntity<ReporterLinkResponse> getByToken(@PathVariable String token,
      Authentication auth) {
    return reporterLinkService.findByToken(token)
        .filter(link -> canAccess(link, auth))
        .map(ReporterLinkResponse::from)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Revokes (deletes) the reporter link with the given token. ADMIN only.
   *
   * @param token the link token
   * @return 204 on success, or 404 if not found
   */
  @DeleteMapping("/{token}")
  public ResponseEntity<Void> delete(@PathVariable String token) {
    if (!reporterLinkService.existsByToken(token)) {
      return ResponseEntity.notFound().build();
    }
    reporterLinkService.deleteByToken(token);
    return ResponseEntity.noContent().build();
  }

  /**
   * Submits a new service instance via the reporter link identified by the given token.
   * The service template and church are resolved from the link itself. Only the reporter
   * named in the link may submit. The link must be active (its activeDate must be today or past),
   * and it is revoked upon successful submission.
   *
   * @param token   the link token
   * @param request the submission data (celebrants, date, responses)
   * @param auth    the current authentication
   * @return 201 with the created service instance identifier and next pending reporter link
   */
  @PostMapping("/{token}/submit")
  public ResponseEntity<ReportSubmissionResponse> submit(@PathVariable String token,
      @RequestBody @Valid ReporterLinkSubmitRequest request, Authentication auth) {
    DashboardUser user = ((DashboardUserDetails) auth.getPrincipal()).getDashboardUser();
    ReporterLink link = reporterLinkService.findByToken(token)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Reporter link not found"));
    if (!link.getReporter().getId().equals(user.getId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "This link does not belong to the authenticated user");
    }
    if (!user.isAssignedToChurch(link.getChurch())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "Reporter account is not assigned to this church");
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
        link.getServiceTemplate().getId(), instanceRequest, user);
    reporterLinkService.deleteByToken(token);
    var nextReporterLink = reporterLinkService.findNextPendingLinkForReporter(user);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ReportSubmissionResponse.from(created, nextReporterLink));
  }

  private boolean canAccess(ReporterLink link, Authentication auth) {
    DashboardUser user = ((DashboardUserDetails) auth.getPrincipal()).getDashboardUser();
    if (user.getRole() == UserRole.ADMIN) {
      return true;
    }
    return link.getReporter().getId().equals(user.getId());
  }
}
