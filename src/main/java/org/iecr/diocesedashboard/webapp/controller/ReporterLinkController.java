package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.Valid;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ReporterLink;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.UserRole;
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

import java.util.List;

/** REST controller for managing and using reporter short-URL links. */
@RestController
@RequestMapping("/api/reporter-links")
public class ReporterLinkController {

  private final ReporterLinkService reporterLinkService;
  private final UserService userService;
  private final ServiceTemplateService serviceTemplateService;
  private final ServiceSubmissionService serviceSubmissionService;

  @Autowired
  public ReporterLinkController(ReporterLinkService reporterLinkService,
      UserService userService,
      ServiceTemplateService serviceTemplateService,
      ServiceSubmissionService serviceSubmissionService) {
    this.reporterLinkService = reporterLinkService;
    this.userService = userService;
    this.serviceTemplateService = serviceTemplateService;
    this.serviceSubmissionService = serviceSubmissionService;
  }

  /**
   * Returns all reporter links. ADMIN only.
   *
   * @return list of all reporter links
   */
  @GetMapping
  public List<ReporterLink> getAll() {
    return reporterLinkService.findAll();
  }

  /**
   * Creates a new reporter link for a specific REPORTER user and service template. ADMIN only.
   *
   * @param request the creation request containing reporterId and serviceTemplateId
   * @return 201 with the created {@link ReporterLink}
   */
  @PostMapping
  public ResponseEntity<ReporterLink> create(@RequestBody @Valid ReporterLinkRequest request) {
    DashboardUser reporter = userService.findById(request.reporterId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "User not found: " + request.reporterId()));
    if (reporter.getRole() != UserRole.REPORTER) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Target user must have the REPORTER role");
    }
    var template = serviceTemplateService.findById(request.serviceTemplateId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Service template not found: " + request.serviceTemplateId()));
    ReporterLink created = reporterLinkService.createLink(reporter, template);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
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
  public ResponseEntity<ReporterLink> getByToken(@PathVariable String token, Authentication auth) {
    return reporterLinkService.findByToken(token)
        .filter(link -> canAccess(link, auth))
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
   * The service template is resolved from the link; the church is taken from the
   * authenticated reporter's profile. Only the reporter named in the link may submit.
   *
   * @param token   the link token
   * @param request the submission data (celebrants, date, responses)
   * @param auth    the current authentication
   * @return 201 with the created service instance
   */
  @PostMapping("/{token}/submit")
  public ResponseEntity<ServiceInstance> submit(@PathVariable String token,
      @RequestBody @Valid ReporterLinkSubmitRequest request, Authentication auth) {
    DashboardUser user = ((DashboardUserDetails) auth.getPrincipal()).getDashboardUser();
    ReporterLink link = reporterLinkService.findByToken(token)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Reporter link not found"));
    if (!link.getReporter().getId().equals(user.getId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "This link does not belong to the authenticated user");
    }
    if (user.getChurch() == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "Reporter account has no church assigned");
    }
    ServiceInstanceRequest instanceRequest = new ServiceInstanceRequest(
        user.getChurch().getName(),
        request.celebrantIds(),
        request.serviceDate(),
        request.responses());
    ServiceInstance created = serviceSubmissionService.submit(
        link.getServiceTemplate().getId(), instanceRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  private boolean canAccess(ReporterLink link, Authentication auth) {
    DashboardUser user = ((DashboardUserDetails) auth.getPrincipal()).getDashboardUser();
    if (user.getRole() == UserRole.ADMIN) {
      return true;
    }
    return link.getReporter().getId().equals(user.getId());
  }
}
