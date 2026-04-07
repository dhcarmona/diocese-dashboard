package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ReporterLink;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.repositories.ReporterLinkRepository;
import org.iecr.diocesedashboard.domain.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Service for managing reporter short-URL links. */
@Service
public class ReporterLinkService {

  private static final Locale WHATSAPP_LOCALE = Locale.forLanguageTag("es");

  private final ReporterLinkRepository reporterLinkRepository;
  private final UserRepository userRepository;
  private final WhatsAppService whatsAppService;
  private final MessageSource messageSource;
  private final String appBaseUrl;

  @Autowired
  public ReporterLinkService(ReporterLinkRepository reporterLinkRepository,
      UserRepository userRepository, WhatsAppService whatsAppService,
      MessageSource messageSource,
      @Value("${app.base-url}") String appBaseUrl) {
    this.reporterLinkRepository = reporterLinkRepository;
    this.userRepository = userRepository;
    this.whatsAppService = whatsAppService;
    this.messageSource = messageSource;
    this.appBaseUrl = appBaseUrl;
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
   * A WhatsApp notification is sent to each reporter with their unique link.
   *
   * @param churches        the list of churches to create links for
   * @param serviceTemplate the service template to associate with each link
   * @param activeDate      the date on which the links become active
   * @return a pair where the first element is the list of created links and the second is
   *         the list of church names that had no reporter assigned
   */
  @Transactional
  public BulkCreateResult createLinksForChurches(List<Church> churches,
      ServiceTemplate serviceTemplate, LocalDate activeDate) {
    List<ReporterLink> created = new ArrayList<>();
    List<String> skipped = new ArrayList<>();

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
      sendLinkNotification(reporter, link.getToken(), activeDate,
          serviceTemplate.getServiceTemplateName());
    }

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
      LocalDate activeDate, String templateName) {
    String phone = reporter.getPhoneNumber();
    if (phone == null || phone.isBlank()) {
      return;
    }
    String linkUrl = appBaseUrl + "/r/" + token;
    String message = messageSource.getMessage(
        "reporter.link.whatsapp.message",
        new Object[]{templateName, activeDate, linkUrl},
        "Tiene un nuevo enlace de reporte para \"" + templateName + "\" ("
            + activeDate + "): " + linkUrl,
        WHATSAPP_LOCALE);
    try {
      whatsAppService.sendMessage(phone, message);
    } catch (Exception ex) {
      // Best-effort: log and continue if WhatsApp delivery fails
    }
  }

  /** Holds the result of a bulk link creation operation. */
  public record BulkCreateResult(List<ReporterLink> created, List<String> skippedChurches) {
  }
}
