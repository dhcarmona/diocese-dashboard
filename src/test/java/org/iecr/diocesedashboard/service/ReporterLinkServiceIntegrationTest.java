package org.iecr.diocesedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

import org.iecr.diocesedashboard.DioceseDashboardApplication;
import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.domain.objects.WhatsAppMessageLog;
import org.iecr.diocesedashboard.domain.repositories.ChurchRepository;
import org.iecr.diocesedashboard.domain.repositories.ReporterLinkRepository;
import org.iecr.diocesedashboard.domain.repositories.ServiceTemplateRepository;
import org.iecr.diocesedashboard.domain.repositories.UserRepository;
import org.iecr.diocesedashboard.domain.repositories.WhatsAppMessageLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Integration tests for {@link ReporterLinkService} that verify behavior across the
 * full transaction lifecycle, including the {@code afterCommit} callback.
 *
 * <p>These tests must NOT be annotated with {@code @Transactional}: if the test method
 * runs inside a transaction, {@code afterCommit} fires only when that outer transaction
 * commits (after the test method returns), which means assertions would run before the
 * callback executes and the log entry would never be visible during the test.
 */
@SpringBootTest(
    classes = DioceseDashboardApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:reporterlinkservicetest;"
            + "MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "dashboard.bootstrap-admin.enabled=false"
    })
class ReporterLinkServiceIntegrationTest {

  @Autowired
  private ReporterLinkService reporterLinkService;

  @SpyBean
  private WhatsAppService whatsAppService;

  @Autowired
  private ChurchRepository churchRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ServiceTemplateRepository serviceTemplateRepository;

  @Autowired
  private ReporterLinkRepository reporterLinkRepository;

  @Autowired
  private WhatsAppMessageLogRepository messageLogRepository;

  private Church church;
  private DashboardUser reporter;
  private ServiceTemplate template;

  @BeforeEach
  void setUp() {
    doNothing().when(whatsAppService).dispatchTextMessage(any(), any());

    church = new Church();
    church.setName("Integration Test Church");
    churchRepository.save(church);

    reporter = new DashboardUser();
    reporter.setUsername("it_reporter");
    reporter.setRole(UserRole.REPORTER);
    reporter.setPhoneNumber("+50688880000");
    reporter.setAssignedChurches(Set.of(church));
    userRepository.save(reporter);

    template = new ServiceTemplate();
    template.setServiceTemplateName("Integration Test Template");
    serviceTemplateRepository.save(template);
  }

  @AfterEach
  void tearDown() {
    messageLogRepository.deleteAll();
    reporterLinkRepository.deleteAll();
    userRepository.delete(reporter);
    serviceTemplateRepository.delete(template);
    churchRepository.delete(church);
  }

  @Test
  void createLinksForChurches_persistsMessageLogAfterCommit() {
    reporterLinkService.createLinksForChurches(
        List.of(church), template, LocalDate.now(), "http://testserver");

    List<WhatsAppMessageLog> logs = messageLogRepository.findAll();
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0).getRecipientUsername()).isEqualTo("it_reporter");
    assertThat(logs.get(0).getBody()).contains("Integration Test Template");
    assertThat(logs.get(0).isOtp()).isFalse();
    assertThat(logs.get(0).getSentAt()).isNotNull();
  }

  @Test
  void createLinksForChurches_doesNotLogWhenReporterHasNoPhone() {
    reporter.setPhoneNumber(null);
    userRepository.save(reporter);

    reporterLinkService.createLinksForChurches(
        List.of(church), template, LocalDate.now(), "http://testserver");

    assertThat(messageLogRepository.findAll()).isEmpty();
  }
}
