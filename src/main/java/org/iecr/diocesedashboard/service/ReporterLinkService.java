package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ReporterLink;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.repositories.ReporterLinkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Service for managing reporter short-URL links. */
@Service
public class ReporterLinkService {

  private final ReporterLinkRepository reporterLinkRepository;

  @Autowired
  public ReporterLinkService(ReporterLinkRepository reporterLinkRepository) {
    this.reporterLinkRepository = reporterLinkRepository;
  }

  /**
   * Creates and persists a new {@link ReporterLink} for the given reporter and service template.
   * A unique UUID token is generated automatically.
   *
   * @param reporter        the REPORTER user to link
   * @param church          the church this link is valid for
   * @param serviceTemplate the service template to associate with the link
   * @return the saved {@link ReporterLink}
   */
  public ReporterLink createLink(DashboardUser reporter, Church church,
      ServiceTemplate serviceTemplate) {
    ReporterLink link = new ReporterLink();
    link.setToken(UUID.randomUUID().toString());
    link.setReporter(reporter);
    link.setChurch(church);
    link.setServiceTemplate(serviceTemplate);
    return reporterLinkRepository.save(link);
  }

  /**
   * Finds a reporter link by its token.
   *
   * @param token the unique token string
   * @return an {@link Optional} containing the link if found
   */
  public Optional<ReporterLink> findByToken(String token) {
    return reporterLinkRepository.findByToken(token);
  }

  public List<ReporterLink> findAll() {
    return reporterLinkRepository.findAll();
  }

  public boolean existsByToken(String token) {
    return reporterLinkRepository.existsByToken(token);
  }

  /**
   * Deletes the reporter link with the given token.
   *
   * @param token the token of the link to delete
   */
  @Transactional
  public void deleteByToken(String token) {
    reporterLinkRepository.deleteByToken(token);
  }
}
