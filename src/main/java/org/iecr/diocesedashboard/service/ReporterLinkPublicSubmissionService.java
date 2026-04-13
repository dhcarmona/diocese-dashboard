package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ReporterLink;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.repositories.ReporterLinkRepository;
import org.iecr.diocesedashboard.webapp.controller.ServiceInstanceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Orchestrates public (unauthenticated) reporter link submission.
 * Wraps token claim and service instance creation in a single transaction so that
 * a submission failure rolls back the token deletion, preserving the reporter's
 * ability to retry.
 */
@Service
public class ReporterLinkPublicSubmissionService {

  private final ReporterLinkRepository reporterLinkRepository;
  private final ServiceSubmissionService serviceSubmissionService;

  @Autowired
  public ReporterLinkPublicSubmissionService(ReporterLinkRepository reporterLinkRepository,
      ServiceSubmissionService serviceSubmissionService) {
    this.reporterLinkRepository = reporterLinkRepository;
    this.serviceSubmissionService = serviceSubmissionService;
  }

  /**
   * Atomically claims the reporter link token and submits a new service instance in a
   * single transaction. If the token has already been claimed by a concurrent request,
   * an empty {@link Optional} is returned. A submission failure rolls back the token
   * deletion, allowing the reporter to retry.
   *
   * @param link            the reporter link to submit against
   * @param token           the reporter link token used for the atomic delete
   * @param instanceRequest the submission data (celebrants, date, responses)
   * @param reporter        the reporter user performing the submission
   * @return the created {@link ServiceInstance}, or empty if the token was already claimed
   */
  @Transactional
  public Optional<ServiceInstance> claimAndSubmit(ReporterLink link, String token,
      ServiceInstanceRequest instanceRequest, DashboardUser reporter) {
    if (reporterLinkRepository.deleteByTokenReturningCount(token) == 0) {
      return Optional.empty();
    }
    return Optional.of(serviceSubmissionService.submit(
        link.getServiceTemplate().getId(), instanceRequest, reporter));
  }
}
