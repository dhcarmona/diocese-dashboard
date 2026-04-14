package org.iecr.diocesedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ReporterLink;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.domain.repositories.ReporterLinkRepository;
import org.iecr.diocesedashboard.domain.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ReporterLinkServiceTest {

  @Mock
  private ReporterLinkRepository reporterLinkRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private WhatsAppService whatsAppService;

  @Mock
  private MessageSource messageSource;

  private ReporterLinkService reporterLinkService;

  @BeforeEach
  void setUp() {
    reporterLinkService = new ReporterLinkService(
        reporterLinkRepository, userRepository, whatsAppService, messageSource);
  }

  private DashboardUser buildReporter() {
    DashboardUser user = new DashboardUser();
    user.setId(5L);
    user.setUsername("reporter1");
    user.setRole(UserRole.REPORTER);
    return user;
  }

  private ServiceTemplate buildTemplate() {
    ServiceTemplate template = new ServiceTemplate();
    template.setId(2L);
    template.setServiceTemplateName("Sunday Mass");
    return template;
  }

  private Church buildChurch() {
    Church church = new Church();
    church.setName("Trinity");
    return church;
  }

  @Test
  void createLink_savesLinkWithGeneratedToken() {
    DashboardUser reporter = buildReporter();
    Church church = buildChurch();
    ServiceTemplate template = buildTemplate();
    ReporterLink saved = new ReporterLink();
    saved.setId(1L);
    when(reporterLinkRepository.save(any(ReporterLink.class))).thenReturn(saved);

    reporter.setAssignedChurches(Set.of(church));
    ReporterLink result = reporterLinkService.createLink(reporter, church, template,
        LocalDate.now());

    ArgumentCaptor<ReporterLink> captor = ArgumentCaptor.forClass(ReporterLink.class);
    verify(reporterLinkRepository).save(captor.capture());
    ReporterLink persisted = captor.getValue();
    assertThat(persisted.getToken()).isNotBlank();
    assertThat(persisted.getReporter()).isEqualTo(reporter);
    assertThat(persisted.getChurch()).isEqualTo(church);
    assertThat(persisted.getServiceTemplate()).isEqualTo(template);
    assertThat(result.getId()).isEqualTo(1L);
  }

  @Test
  void createLink_tokenIsValidUuid() {
    when(reporterLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ReporterLink result = reporterLinkService.createLink(
        buildReporter(), buildChurch(), buildTemplate(), LocalDate.now());

    // should not throw — UUID.fromString validates the format
    UUID.fromString(result.getToken());
  }

  @Test
  void findByToken_returnsPresent_whenExists() {
    String token = UUID.randomUUID().toString();
    ReporterLink link = new ReporterLink();
    link.setToken(token);
    when(reporterLinkRepository.findByToken(token)).thenReturn(Optional.of(link));

    Optional<ReporterLink> result = reporterLinkService.findByToken(token);

    assertThat(result).isPresent();
    assertThat(result.get().getToken()).isEqualTo(token);
  }

  @Test
  void findByToken_returnsEmpty_whenNotExists() {
    when(reporterLinkRepository.findByToken("missing")).thenReturn(Optional.empty());

    assertThat(reporterLinkService.findByToken("missing")).isEmpty();
  }

  @Test
  void findAll_delegatesToRepository() {
    when(reporterLinkRepository.findAll()).thenReturn(List.of(new ReporterLink()));

    List<ReporterLink> result = reporterLinkService.findAll();

    assertThat(result).hasSize(1);
  }

  @Test
  void existsByToken_delegatesToRepository() {
    when(reporterLinkRepository.existsByToken("tok")).thenReturn(true);

    assertThat(reporterLinkService.existsByToken("tok")).isTrue();
  }

  @Test
  void deleteByToken_callsRepository() {
    reporterLinkService.deleteByToken("tok");

    verify(reporterLinkRepository).deleteByToken("tok");
  }

  @Test
  void createLinksForChurches_skipsChurchWithNoReporter() {
    when(userRepository.findReportersByAssignedChurchName("Trinity")).thenReturn(List.of());

    ReporterLinkService.BulkCreateResult result = reporterLinkService.createLinksForChurches(
        List.of(buildChurch()), buildTemplate(), LocalDate.now(), "http://localhost:8080");

    assertThat(result.created()).isEmpty();
    assertThat(result.skippedChurches()).containsExactly("Trinity");
    verify(reporterLinkRepository, never()).save(any());
  }

  @Test
  void createLinksForChurches_createsLinkForEachChurchWithReporter() {
    DashboardUser reporter = buildReporter();
    when(userRepository.findReportersByAssignedChurchName("Trinity"))
        .thenReturn(List.of(reporter));
    when(reporterLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ReporterLinkService.BulkCreateResult result = reporterLinkService.createLinksForChurches(
        List.of(buildChurch()), buildTemplate(), LocalDate.now(), "http://localhost:8080");

    assertThat(result.created()).hasSize(1);
    assertThat(result.skippedChurches()).isEmpty();
    verify(reporterLinkRepository).save(any());
  }

  @Test
  void createLinksForChurches_sendsWhatsAppWhenPhonePresent() {
    DashboardUser reporter = buildReporter();
    reporter.setPhoneNumber("+50688887777");
    reporter.setPreferredLanguage("en");
    LocalDate activeDate = LocalDate.now();
    when(userRepository.findReportersByAssignedChurchName("Trinity"))
        .thenReturn(List.of(reporter));
    when(reporterLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(messageSource.getMessage(any(), any(), any(Locale.class))).thenReturn("link message");

    reporterLinkService.createLinksForChurches(
        List.of(buildChurch()), buildTemplate(), activeDate, "http://testserver");

    ArgumentCaptor<Object[]> messageArguments = ArgumentCaptor.forClass(Object[].class);
    verify(whatsAppService).sendMessageAndLog(eq("+50688887777"), eq("link message"),
        contains("Sunday Mass"), eq("reporter1"));
    verify(messageSource).getMessage(
        eq("reporter.link.whatsapp.message"), messageArguments.capture(), eq(Locale.ENGLISH));
    Object[] localizedArguments = messageArguments.getValue();
    assertThat(localizedArguments[0]).isEqualTo("Sunday Mass");
    assertThat(localizedArguments[1]).isEqualTo("Trinity");
    assertThat(localizedArguments[2]).isEqualTo(activeDate);
    assertThat(localizedArguments[3]).isInstanceOf(String.class);
    assertThat((String) localizedArguments[3]).startsWith("http://testserver/r/");
  }

  @Test
  void createLinksForChurches_skipsNotificationWhenNoPhone() {
    DashboardUser reporter = buildReporter();
    reporter.setPhoneNumber(null);
    when(userRepository.findReportersByAssignedChurchName("Trinity"))
        .thenReturn(List.of(reporter));
    when(reporterLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    reporterLinkService.createLinksForChurches(
        List.of(buildChurch()), buildTemplate(), LocalDate.now(), "http://testserver");

    verify(whatsAppService, never()).sendMessageAndLog(any(), any(), any(), any());
  }

  @Test
  void createLinksForChurches_whatsAppFailureDoesNotAbortPersistence() {
    DashboardUser reporter = buildReporter();
    reporter.setPhoneNumber("+50688887777");
    when(userRepository.findReportersByAssignedChurchName("Trinity"))
        .thenReturn(List.of(reporter));
    when(reporterLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(messageSource.getMessage(any(), any(), any(Locale.class))).thenReturn("msg");
    Mockito.doThrow(new RuntimeException("network error"))
        .when(whatsAppService).sendMessageAndLog(any(), any(), any(), any());

    ReporterLinkService.BulkCreateResult result = reporterLinkService.createLinksForChurches(
        List.of(buildChurch()), buildTemplate(), LocalDate.now(), "http://testserver");

    assertThat(result.created()).hasSize(1);
  }
}
