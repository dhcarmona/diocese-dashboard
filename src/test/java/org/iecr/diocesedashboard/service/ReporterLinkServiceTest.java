package org.iecr.diocesedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ReporterLink;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.domain.repositories.ReporterLinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ReporterLinkServiceTest {

  @Mock
  private ReporterLinkRepository reporterLinkRepository;

  @InjectMocks
  private ReporterLinkService reporterLinkService;

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
    ReporterLink result = reporterLinkService.createLink(reporter, church, template);

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
        buildReporter(), buildChurch(), buildTemplate());

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
}
