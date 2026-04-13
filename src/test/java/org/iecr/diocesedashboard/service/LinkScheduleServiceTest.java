package org.iecr.diocesedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.LinkSchedule;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.repositories.LinkScheduleRepository;
import org.iecr.diocesedashboard.webapp.controller.LinkScheduleRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class LinkScheduleServiceTest {

  @Mock
  private LinkScheduleRepository repository;
  @Mock
  private ServiceTemplateService serviceTemplateService;
  @Mock
  private ChurchService churchService;
  @Mock
  private ReporterLinkService reporterLinkService;

  @InjectMocks
  private LinkScheduleService linkScheduleService;

  @Test
  void create_persistsScheduleWithCorrectFields() {
    ServiceTemplate template = new ServiceTemplate();
    template.setId(1L);
    template.setServiceTemplateName("Sunday Eucharist");
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(template));

    LinkScheduleRequest request = new LinkScheduleRequest(
        1L,
        List.of("Church A", "Church B"),
        List.of(DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY),
        8);

    ArgumentCaptor<LinkSchedule> captor = ArgumentCaptor.forClass(LinkSchedule.class);
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    LinkSchedule result = linkScheduleService.create(request);

    verify(repository).save(captor.capture());
    LinkSchedule saved = captor.getValue();
    assertThat(saved.getServiceTemplate()).isEqualTo(template);
    assertThat(saved.getSendHour()).isEqualTo(8);
    assertThat(saved.getDaysOfWeek()).containsExactlyInAnyOrder(
        DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY);
    assertThat(saved.getChurchNames()).containsExactlyInAnyOrder("Church A", "Church B");
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(result).isSameAs(saved);
  }

  @Test
  void create_throws404_whenTemplateNotFound() {
    when(serviceTemplateService.findById(99L)).thenReturn(Optional.empty());

    LinkScheduleRequest request = new LinkScheduleRequest(
        99L, List.of("Church A"), List.of(DayOfWeek.MONDAY), 9);

    assertThatThrownBy(() -> linkScheduleService.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("99");

    verify(repository, never()).save(any());
  }

  @Test
  void update_modifiesExistingSchedule() {
    LinkSchedule existing = new LinkSchedule();
    existing.setId(5L);
    existing.setCreatedAt(Instant.EPOCH);
    when(repository.findById(5L)).thenReturn(Optional.of(existing));

    ServiceTemplate template = new ServiceTemplate();
    template.setId(2L);
    template.setServiceTemplateName("Evening Prayer");
    when(serviceTemplateService.findById(2L)).thenReturn(Optional.of(template));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    LinkScheduleRequest request = new LinkScheduleRequest(
        2L, List.of("Church C"), List.of(DayOfWeek.FRIDAY), 18);

    LinkSchedule result = linkScheduleService.update(5L, request);

    assertThat(result.getSendHour()).isEqualTo(18);
    assertThat(result.getDaysOfWeek()).containsExactly(DayOfWeek.FRIDAY);
    assertThat(result.getChurchNames()).containsExactly("Church C");
    assertThat(result.getServiceTemplate()).isEqualTo(template);
    assertThat(result.getCreatedAt()).isEqualTo(Instant.EPOCH);
  }

  @Test
  void update_throws404_whenScheduleNotFound() {
    when(repository.findById(99L)).thenReturn(Optional.empty());

    LinkScheduleRequest request = new LinkScheduleRequest(
        1L, List.of("Church A"), List.of(DayOfWeek.MONDAY), 8);

    assertThatThrownBy(() -> linkScheduleService.update(99L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("99");
  }

  @Test
  void delete_deletesById_whenExists() {
    when(repository.existsById(3L)).thenReturn(true);

    linkScheduleService.delete(3L);

    verify(repository).deleteById(3L);
  }

  @Test
  void delete_throws404_whenNotFound() {
    when(repository.existsById(99L)).thenReturn(false);

    assertThatThrownBy(() -> linkScheduleService.delete(99L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("99");

    verify(repository, never()).deleteById(any());
  }

  @Test
  void executeSchedule_createsLinksAndUpdatesLastTriggeredDate() {
    Church churchA = new Church();
    churchA.setName("Church A");
    Church churchB = new Church();
    churchB.setName("Church B");

    ServiceTemplate template = new ServiceTemplate();
    template.setId(1L);

    LinkSchedule schedule = new LinkSchedule();
    schedule.setId(1L);
    schedule.setServiceTemplate(template);
    schedule.setChurchNames(Set.of("Church A", "Church B"));
    schedule.setSendHour(8);
    schedule.setDaysOfWeek(Set.of(DayOfWeek.SUNDAY));

    when(churchService.findAllById(anyIterable())).thenReturn(List.of(churchA, churchB));
    when(reporterLinkService.createLinksForChurches(any(), any(), any(), any()))
        .thenReturn(new ReporterLinkService.BulkCreateResult(List.of(), List.of()));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    LocalDate today = LocalDate.of(2024, 4, 15);
    linkScheduleService.executeSchedule(schedule, "https://example.com", today);

    verify(reporterLinkService).createLinksForChurches(
        any(), eq(template), eq(today), eq("https://example.com"));
    assertThat(schedule.getLastTriggeredDate()).isEqualTo(today);
    verify(repository).save(schedule);
  }

  @Test
  void executeSchedule_skips_whenNoChurchesResolvable() {
    LinkSchedule schedule = new LinkSchedule();
    schedule.setId(2L);
    schedule.setChurchNames(Set.of("Ghost Church"));
    schedule.setDaysOfWeek(Set.of(DayOfWeek.MONDAY));

    when(churchService.findAllById(anyIterable())).thenReturn(List.of());

    linkScheduleService.executeSchedule(schedule, "https://example.com", LocalDate.of(2024, 4, 15));

    verify(reporterLinkService, never()).createLinksForChurches(any(), any(), any(), any());
    verify(repository, never()).save(any());
  }
}
