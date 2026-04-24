package org.iecr.diocesedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import org.iecr.diocesedashboard.domain.objects.Celebrant;
import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ReporterLink;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemResponse;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemType;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.domain.repositories.ReporterLinkRepository;
import org.iecr.diocesedashboard.domain.repositories.ServiceInfoItemResponseRepository;
import org.iecr.diocesedashboard.domain.repositories.ServiceInstanceRepository;
import org.iecr.diocesedashboard.webapp.controller.StatisticsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

  @Mock
  private ServiceInstanceRepository instanceRepository;

  @Mock
  private ServiceInfoItemResponseRepository responseRepository;

  @Mock
  private ReporterLinkRepository reporterLinkRepository;

  @Mock
  private ChurchService churchService;

  @InjectMocks
  private StatisticsService statisticsService;

  private Church church;
  private ServiceTemplate template;
  private ServiceInfoItem numericalItem;
  private ServiceInfoItem moneyItem;
  private ServiceInfoItem stringItem;

  private static final LocalDate START = LocalDate.of(2024, 1, 1);
  private static final LocalDate END = LocalDate.of(2024, 12, 31);

  @BeforeEach
  void setUp() {
    church = new Church();
    church.setName("Trinity");

    template = new ServiceTemplate();
    template.setId(1L);
    template.setServiceTemplateName("Sunday Mass");

    numericalItem = new ServiceInfoItem();
    numericalItem.setId(10L);
    numericalItem.setTitle("Attendance");
    numericalItem.setServiceInfoItemType(ServiceInfoItemType.NUMERICAL);
    numericalItem.setServiceTemplate(template);

    moneyItem = new ServiceInfoItem();
    moneyItem.setId(11L);
    moneyItem.setTitle("Offering");
    moneyItem.setServiceInfoItemType(ServiceInfoItemType.DOLLARS);
    moneyItem.setServiceTemplate(template);

    stringItem = new ServiceInfoItem();
    stringItem.setId(12L);
    stringItem.setTitle("Notes");
    stringItem.setServiceInfoItemType(ServiceInfoItemType.STRING);
    stringItem.setServiceTemplate(template);

    template.setServiceInfoItems(List.of(numericalItem, moneyItem, stringItem));
  }

  private ServiceInstance buildInstance(LocalDate date) {
    ServiceInstance instance = new ServiceInstance();
    instance.setChurch(church);
    instance.setServiceTemplate(template);
    instance.setServiceDate(date);
    return instance;
  }

  private ServiceInfoItemResponse buildResponse(ServiceInfoItem item,
      ServiceInstance instance, String value) {
    ServiceInfoItemResponse response = new ServiceInfoItemResponse();
    response.setServiceInfoItem(item);
    response.setServiceInstance(instance);
    response.setResponseValue(value);
    return response;
  }

  @Test
  void computeForChurch_returnsTotalServiceCount() {
    ServiceInstance inst1 = buildInstance(LocalDate.of(2024, 3, 10));
    ServiceInstance inst2 = buildInstance(LocalDate.of(2024, 6, 5));

    when(instanceRepository.findByTemplateAndChurchAndDateRangeWithCelebrants(
        template, church, START, END)).thenReturn(List.of(inst1, inst2));
    when(reporterLinkRepository.findByChurchAndServiceTemplate(church, template))
        .thenReturn(List.of());
    when(responseRepository.findByServiceInstanceInWithItems(List.of(inst1, inst2))).thenReturn(List.of());

    StatisticsResponse result = statisticsService.computeForChurch(template, church, START, END,
        null);

    assertThat(result.totalServiceCount()).isEqualTo(2);
    assertThat(result.churchName()).isEqualTo("Trinity");
    assertThat(result.global()).isFalse();
  }

  @Test
  void computeForChurch_aggregatesNumericalItemsAndIgnoresString() {
    ServiceInstance inst = buildInstance(LocalDate.of(2024, 3, 10));

    when(instanceRepository.findByTemplateAndChurchAndDateRangeWithCelebrants(
        template, church, START, END)).thenReturn(List.of(inst));
    when(reporterLinkRepository.findByChurchAndServiceTemplate(church, template))
        .thenReturn(List.of());
    when(responseRepository.findByServiceInstanceInWithItems(List.of(inst))).thenReturn(List.of(
        buildResponse(numericalItem, inst, "120"),
        buildResponse(moneyItem, inst, "500.50"),
        buildResponse(stringItem, inst, "some notes")
    ));

    StatisticsResponse result = statisticsService.computeForChurch(template, church, START, END,
        null);

    assertThat(result.numericalItems()).hasSize(1);
    assertThat(result.numericalItems().get(0).itemTitle()).isEqualTo("Attendance");
    assertThat(result.numericalItems().get(0).total()).isEqualTo(120.0);

    assertThat(result.moneyItems()).hasSize(1);
    assertThat(result.moneyItems().get(0).itemTitle()).isEqualTo("Offering");
    assertThat(result.moneyItems().get(0).total()).isEqualTo(500.50);
  }

  @Test
  void computeForChurch_accumulatesTotalsAcrossMultipleInstances() {
    ServiceInstance inst1 = buildInstance(LocalDate.of(2024, 1, 7));
    ServiceInstance inst2 = buildInstance(LocalDate.of(2024, 1, 14));

    when(instanceRepository.findByTemplateAndChurchAndDateRangeWithCelebrants(
        template, church, START, END)).thenReturn(List.of(inst1, inst2));
    when(reporterLinkRepository.findByChurchAndServiceTemplate(church, template))
        .thenReturn(List.of());
    when(responseRepository.findByServiceInstanceInWithItems(anyList())).thenReturn(List.of(
        buildResponse(numericalItem, inst1, "80"),
        buildResponse(numericalItem, inst2, "95")
    ));

    StatisticsResponse result = statisticsService.computeForChurch(template, church, START, END,
        null);

    assertThat(result.numericalItems().get(0).total()).isEqualTo(175.0);
    assertThat(result.numericalItems().get(0).timeSeriesData()).hasSize(2);
  }

  @Test
  void computeForChurch_buildsCelebrantStats() {
    Celebrant alice = new Celebrant();
    alice.setId(1L);
    alice.setName("Alice");
    Celebrant bob = new Celebrant();
    bob.setId(2L);
    bob.setName("Bob");

    ServiceInstance inst1 = buildInstance(LocalDate.of(2024, 1, 7));
    inst1.setCelebrants(Set.of(alice));
    ServiceInstance inst2 = buildInstance(LocalDate.of(2024, 1, 14));
    inst2.setCelebrants(Set.of(alice));
    ServiceInstance inst3 = buildInstance(LocalDate.of(2024, 1, 21));
    inst3.setCelebrants(Set.of(bob));

    when(instanceRepository.findByTemplateAndChurchAndDateRangeWithCelebrants(
        template, church, START, END)).thenReturn(List.of(inst1, inst2, inst3));
    when(reporterLinkRepository.findByChurchAndServiceTemplate(church, template))
        .thenReturn(List.of());
    when(responseRepository.findByServiceInstanceInWithItems(anyList())).thenReturn(List.of());

    StatisticsResponse result = statisticsService.computeForChurch(template, church, START, END,
        null);

    assertThat(result.celebrantStats()).hasSize(2);
    assertThat(result.celebrantStats().get(0).celebrantName()).isEqualTo("Alice");
    assertThat(result.celebrantStats().get(0).serviceCount()).isEqualTo(2);
    assertThat(result.celebrantStats().get(1).celebrantName()).isEqualTo("Bob");
    assertThat(result.celebrantStats().get(1).serviceCount()).isEqualTo(1);
  }

  @Test
  void computeForChurch_pendingLinks_showsAllExistingLinks() {
    LocalDate today = LocalDate.now();
    LocalDate future = today.plusDays(7);
    LocalDate past = today.minusDays(1);

    DashboardUser reporter = new DashboardUser();
    reporter.setUsername("rep1");
    reporter.setRole(UserRole.REPORTER);

    ReporterLink futureLink = buildLink(reporter, future);
    ReporterLink pastLink = buildLink(reporter, past);

    when(instanceRepository.findByTemplateAndChurchAndDateRangeWithCelebrants(
        template, church, START, END)).thenReturn(List.of());
    when(reporterLinkRepository.findByChurchAndServiceTemplate(church, template))
        .thenReturn(List.of(futureLink, pastLink));

    StatisticsResponse result = statisticsService.computeForChurch(template, church, START, END,
        null);

    // Both links exist in the DB → both are pending (links are deleted on use/admin-delete)
    assertThat(result.pendingLinks()).hasSize(2);
  }

  @Test
  void computeGlobal_setsGlobalFlagAndNullChurchName() {
    when(instanceRepository.findByTemplateAndDateRangeWithCelebrants(template, START, END))
        .thenReturn(List.of());
    when(churchService.findAll()).thenReturn(List.of(church));
    when(reporterLinkRepository.findByChurchInAndServiceTemplate(List.of(church), template))
        .thenReturn(List.of());

    StatisticsResponse result = statisticsService.computeGlobal(template, START, END);

    assertThat(result.global()).isTrue();
    assertThat(result.churchName()).isNull();
  }

  @Test
  void computeForChurch_toleratesBlankResponseValue() {
    ServiceInstance inst = buildInstance(LocalDate.of(2024, 5, 1));
    when(instanceRepository.findByTemplateAndChurchAndDateRangeWithCelebrants(
        template, church, START, END)).thenReturn(List.of(inst));
    when(reporterLinkRepository.findByChurchAndServiceTemplate(church, template))
        .thenReturn(List.of());
    when(responseRepository.findByServiceInstanceInWithItems(List.of(inst))).thenReturn(
        List.of(buildResponse(numericalItem, inst, ""),
            buildResponse(numericalItem, inst, "not-a-number")));

    StatisticsResponse result = statisticsService.computeForChurch(template, church, START, END,
        null);

    assertThat(result.numericalItems().get(0).total()).isEqualTo(0.0);
  }

  @Test
  void computeForChurch_pendingLinksAreSortedByActiveDate() {
    LocalDate today = LocalDate.now();
    DashboardUser reporter = new DashboardUser();
    reporter.setUsername("rep1");
    reporter.setRole(UserRole.REPORTER);

    ReporterLink link1 = buildLink(reporter, today.plusDays(14));
    ReporterLink link2 = buildLink(reporter, today.plusDays(3));
    ReporterLink link3 = buildLink(reporter, today.plusDays(21));

    when(instanceRepository.findByTemplateAndChurchAndDateRangeWithCelebrants(
        template, church, START, END)).thenReturn(List.of());
    when(reporterLinkRepository.findByChurchAndServiceTemplate(church, template))
        .thenReturn(List.of(link1, link2, link3));

    StatisticsResponse result = statisticsService.computeForChurch(template, church, START, END,
        null);

    assertThat(result.pendingLinks()).hasSize(3);
    assertThat(result.pendingLinks().get(0).activeDate()).isEqualTo(today.plusDays(3));
    assertThat(result.pendingLinks().get(1).activeDate()).isEqualTo(today.plusDays(14));
    assertThat(result.pendingLinks().get(2).activeDate()).isEqualTo(today.plusDays(21));
  }

  @Test
  void computeForChurch_toleratesInstanceWithNullCelebrants() {
    ServiceInstance inst = buildInstance(LocalDate.of(2024, 3, 10));
    inst.setCelebrants(null);

    when(instanceRepository.findByTemplateAndChurchAndDateRangeWithCelebrants(
        template, church, START, END)).thenReturn(List.of(inst));
    when(reporterLinkRepository.findByChurchAndServiceTemplate(church, template))
        .thenReturn(List.of());
    when(responseRepository.findByServiceInstanceInWithItems(List.of(inst))).thenReturn(List.of());

    StatisticsResponse result = statisticsService.computeForChurch(template, church, START, END,
        null);

    assertThat(result.celebrantStats()).isEmpty();
  }

  @Test
  void computeForChurch_timeSeriesPointsAreChronological() {
    ServiceInstance instMar = buildInstance(LocalDate.of(2024, 3, 1));
    ServiceInstance instJan = buildInstance(LocalDate.of(2024, 1, 1));
    ServiceInstance instFeb = buildInstance(LocalDate.of(2024, 2, 1));

    when(instanceRepository.findByTemplateAndChurchAndDateRangeWithCelebrants(
        template, church, START, END)).thenReturn(List.of(instMar, instJan, instFeb));
    when(reporterLinkRepository.findByChurchAndServiceTemplate(church, template))
        .thenReturn(List.of());
    when(responseRepository.findByServiceInstanceInWithItems(anyList())).thenReturn(List.of(
        buildResponse(numericalItem, instMar, "10"),
        buildResponse(numericalItem, instJan, "10"),
        buildResponse(numericalItem, instFeb, "10")
    ));

    StatisticsResponse result = statisticsService.computeForChurch(template, church, START, END,
        null);

    List<StatisticsResponse.TimeSeriesPoint> series =
        result.numericalItems().get(0).timeSeriesData();
    assertThat(series).hasSize(3);
    assertThat(series.get(0).date()).isEqualTo(LocalDate.of(2024, 1, 1));
    assertThat(series.get(1).date()).isEqualTo(LocalDate.of(2024, 2, 1));
    assertThat(series.get(2).date()).isEqualTo(LocalDate.of(2024, 3, 1));
  }

  @Test
  void computeGlobal_aggregatesNumericalDataAcrossChurches() {
    Church church2 = new Church();
    church2.setName("Grace");

    ServiceInstance inst1 = buildInstance(LocalDate.of(2024, 1, 7));
    ServiceInstance inst2 = new ServiceInstance();
    inst2.setChurch(church2);
    inst2.setServiceTemplate(template);
    inst2.setServiceDate(LocalDate.of(2024, 2, 4));

    when(instanceRepository.findByTemplateAndDateRangeWithCelebrants(template, START, END))
        .thenReturn(List.of(inst1, inst2));
    when(churchService.findAll()).thenReturn(List.of(church, church2));
    when(reporterLinkRepository.findByChurchInAndServiceTemplate(
        List.of(church, church2), template)).thenReturn(List.of());
    when(responseRepository.findByServiceInstanceInWithItems(anyList())).thenReturn(List.of(
        buildResponse(numericalItem, inst1, "50"),
        buildResponse(numericalItem, inst2, "50")
    ));

    StatisticsResponse result = statisticsService.computeGlobal(template, START, END);

    assertThat(result.numericalItems().get(0).total()).isEqualTo(100.0);
  }

  @Test
  void computeForChurch_pendingLinks_filtersToReporterWhenReporterIdProvided() {
    DashboardUser rep1 = new DashboardUser();
    rep1.setId(1L);
    rep1.setUsername("rep1");
    rep1.setRole(UserRole.REPORTER);

    DashboardUser rep2 = new DashboardUser();
    rep2.setId(2L);
    rep2.setUsername("rep2");
    rep2.setRole(UserRole.REPORTER);

    ReporterLink link1 = buildLink(rep1, LocalDate.of(2025, 6, 1));
    ReporterLink link2 = buildLink(rep2, LocalDate.of(2025, 6, 8));

    when(instanceRepository.findByTemplateAndChurchAndDateRangeWithCelebrants(
        template, church, START, END)).thenReturn(List.of());
    when(reporterLinkRepository.findByChurchAndServiceTemplate(church, template))
        .thenReturn(List.of(link1, link2));

    StatisticsResponse result = statisticsService.computeForChurch(template, church, START, END,
        1L);

    // Only rep1's link should be visible when filtering by rep1's ID
    assertThat(result.pendingLinks()).hasSize(1);
    assertThat(result.pendingLinks().get(0).reporterUsername()).isEqualTo("rep1");
  }

  private ReporterLink buildLink(DashboardUser reporter, LocalDate activeDate) {
    ReporterLink link = new ReporterLink();
    link.setToken(UUID.randomUUID().toString());
    link.setReporter(reporter);
    link.setChurch(church);
    link.setServiceTemplate(template);
    link.setActiveDate(activeDate);
    return link;
  }
}
