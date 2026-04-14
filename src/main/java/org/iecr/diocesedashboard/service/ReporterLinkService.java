package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ReporterLink;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.repositories.ReporterLinkRepository;
import org.iecr.diocesedashboard.domain.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Service for managing reporter short-URL links. */
@Service
public class ReporterLinkService {

  private static final Logger logger = LoggerFactory.getLogger(ReporterLinkService.class);

  private final ReporterLinkRepository reporterLinkRepository;
  private final UserRepository userRepository;
  private final WhatsAppService whatsAppService;
  private final MessageSource messageSource;

  @Autowired
  public ReporterLinkService(ReporterLinkRepository reporterLinkRepository,
      UserRepository userRepository, WhatsAppService whatsAppService,
      MessageSource messageSource) {
    this.reporterLinkRepository = reporterLinkRepository;
    this.userRepository = userRepository;
    this.whatsAppService = whatsAppService;
    this.messageSource = messageSource;
  }

  /**
   * Creates and persists a new {@link ReporterLink} for the given reporter and service template.
   * A unique UUID token is generated automatically.
   *
   * @param reporter        the REPORTER user to link
   * @param church          the church this link is valid for
   * @param serviceTemplate the service template to associate with the link
   * @param activeDate      the date on which this link becomes active
   * @return the saved {@link ReporterLink}
   */
  public ReporterLink createLink(DashboardUser reporter, Church church,
      ServiceTemplate serviceTemplate, LocalDate activeDate) {
    ReporterLink link = new ReporterLink();
    link.setToken(UUID.randomUUID().toString());
    link.setReporter(reporter);
    link.setChurch(church);
    link.setServiceTemplate(serviceTemplate);
    link.setActiveDate(activeDate);
    return reporterLinkRepository.save(link);
  }

  /**
   * Bulk-creates reporter links for the given list of churches, resolving the assigned
   * reporter for each. Churches with no assigned reporter are returned in the skipped list.
   * A WhatsApp notification is sent after commit to each reporter with their unique link.
   *
   * @param churches        the list of churches to create links for
   * @param serviceTemplate the service template to associate with each link
   * @param activeDate      the date on which the links become active
   * @param baseUrl         the server base URL used to build the link sent to reporters
   * @return a pair where the first element is the list of created links and the second is
   *         the list of church names that had no reporter assigned
   */
  @Transactional
  public BulkCreateResult createLinksForChurches(List<Church> churches,
      ServiceTemplate serviceTemplate, LocalDate activeDate, String baseUrl) {
    List<ReporterLink> created = new ArrayList<>();
    List<String> skipped = new ArrayList<>();
    List<Runnable> pendingNotifications = new ArrayList<>();

    for (Church church : churches) {
      List<DashboardUser> reporters =
          userRepository.findReportersByAssignedChurchName(church.getName());
      if (reporters.isEmpty()) {
        skipped.add(church.getName());
        continue;
      }
      DashboardUser reporter = reporters.get(0);
      ReporterLink link = createLink(reporter, church, serviceTemplate, activeDate);
      created.add(link);
      String token = link.getToken();
      String templateName = serviceTemplate.getServiceTemplateName();
      pendingNotifications.add(
          () -> sendLinkNotification(reporter, token, activeDate, templateName,
              church.getName(), baseUrl));
    }

    dispatchAfterCommit(pendingNotifications);
    return new BulkCreateResult(created, skipped);
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

  /**
   * Returns the next pending reporter link for the given reporter, ordered by report date.
   *
   * @param reporter the reporter whose next link should be returned
   * @return the oldest remaining reporter link for that reporter, if any
   */
  public Optional<ReporterLink> findNextPendingLinkForReporter(DashboardUser reporter) {
    return reporterLinkRepository.findFirstByReporterOrderByActiveDateAscIdAsc(reporter);
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

  private void sendLinkNotification(DashboardUser reporter, String token,
      LocalDate activeDate, String templateName, String churchName, String baseUrl) {
    String phone = reporter.getPhoneNumber();
    if (phone == null || phone.isBlank()) {
      return;
    }
    String linkUrl = baseUrl + "/r/" + token;
    String message = messageSource.getMessage(
        "reporter.link.whatsapp.message",
        new Object[]{templateName, churchName, activeDate, linkUrl},
        reporter.getPreferredLocale());
    try {
      whatsAppService.sendMessageAndLog(phone, message,
          "Link for \"" + templateName + "\" sent.", reporter.getUsername());
    } catch (Exception ex) {
      logger.warn("WhatsApp delivery failed for reporter {} ({}): {}",
          reporter.getId(), reporter.getUsername(), ex.getMessage());
    }
  }

  private void dispatchAfterCommit(List<Runnable> tasks) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          tasks.forEach(Runnable::run);
        }
      });
    } else {
      tasks.forEach(Runnable::run);
    }
  }

  /** Holds the result of a bulk link creation operation. */
  public record BulkCreateResult(List<ReporterLink> created, List<String> skippedChurches) {
  }
}
