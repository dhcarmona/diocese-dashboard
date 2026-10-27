package org.iecr.diocesedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemResponse;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemType;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.repositories.ServiceInfoItemResponseRepository;
import org.iecr.diocesedashboard.domain.repositories.ServiceInstanceRepository;
import org.iecr.diocesedashboard.webapp.controller.ConventionReportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ConventionReportServiceTest {

  @Mock
  private ServiceInstanceRepository instanceRepository;

  @Mock
  private ServiceInfoItemResponseRepository responseRepository;

  @Mock
  private ChurchService churchService;

  @InjectMocks
  private ConventionReportService conventionReportService;

  private ServiceTemplate template;
  private Church trinity;
  private Church stMarks;
  private ServiceInfoItem numericalItem;
  private ServiceInfoItem moneyItem;
  private ServiceInfoItem stringItem;

  private static final LocalDate START = LocalDate.of(2024, 1, 1);
  private static final LocalDate END = LocalDate.of(2024, 1, 31);

  @BeforeEach
  void setUp() {
    template = new ServiceTemplate();
    template.setId(1L);
    template.setServiceTemplateName("Sunday Mass");

    trinity = new Church();
    trinity.setName("Trinity");

    stMarks = new Church();
    stMarks.setName("St. Mark's");

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

  private ServiceInstance buildInstance(Long id, Church church, LocalDate date) {
    ServiceInstance instance = new ServiceInstance();
    instance.setId(id);
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
  void compute_columnsExcludeStringItemsAndPreserveOrder() {
    when(instanceRepository.findByTemplateAndDateRangeWithCelebrants(template, START, END))
        .thenReturn(List.of());
    when(churchService.findAll()).thenReturn(List.of());

    ConventionReportResponse result = conventionReportService.compute(template, START, END);

    assertThat(result.columns()).hasSize(2);
    assertThat(result.columns().get(0).itemTitle()).isEqualTo("Attendance");
    assertThat(result.columns().get(1).itemTitle()).isEqualTo("Offering");
  }

  @Test
  void compute_includesOneRowPerChurchEvenWithoutReports() {
    when(instanceRepository.findByTemplateAndDateRangeWithCelebrants(template, START, END))
        .thenReturn(List.of());
    when(churchService.findAll()).thenReturn(List.of(trinity, stMarks));

    ConventionReportResponse result = conventionReportService.compute(template, START, END);

    assertThat(result.rows()).hasSize(2);
    assertThat(result.rows().get(0).churchName()).isEqualTo("Trinity");
    assertThat(result.rows().get(0).reportCount()).isEqualTo(0);
    assertThat(result.rows().get(0).values()).containsExactly(0.0, 0.0);
    assertThat(result.rows().get(1).churchName()).isEqualTo("St. Mark's");
  }

  @Test
  void compute_onlyCountsSundaysAndSumsAcrossThem() {
    // Jan 7 2024 and Jan 14 2024 are Sundays; Jan 8 2024 is a Monday.
    LocalDate sunday1 = LocalDate.of(2024, 1, 7);
    LocalDate sunday2 = LocalDate.of(2024, 1, 14);
    LocalDate monday = LocalDate.of(2024, 1, 8);

    ServiceInstance inst1 = buildInstance(1L, trinity, sunday1);
    ServiceInstance inst2 = buildInstance(2L, trinity, sunday2);
    ServiceInstance instMonday = buildInstance(3L, trinity, monday);

    when(instanceRepository.findByTemplateAndDateRangeWithCelebrants(template, START, END))
        .thenReturn(List.of(inst1, inst2, instMonday));
    when(churchService.findAll()).thenReturn(List.of(trinity));
    when(responseRepository.findByServiceInstanceInWithItems(List.of(inst1, inst2))).thenReturn(
        List.of(
            buildResponse(numericalItem, inst1, "100"),
            buildResponse(moneyItem, inst1, "50.00"),
            buildResponse(numericalItem, inst2, "80"),
            buildResponse(moneyItem, inst2, "20.00")));

    ConventionReportResponse result = conventionReportService.compute(template, START, END);

    assertThat(result.rows()).hasSize(1);
    assertThat(result.rows().get(0).reportCount()).isEqualTo(2);
    assertThat(result.rows().get(0).values()).containsExactly(180.0, 70.0);
  }

  @Test
  void compute_duplicateReportsOnSameDate_keepsFirstAndRecordsNote() {
    LocalDate sunday = LocalDate.of(2024, 1, 7);
    ServiceInstance earlier = buildInstance(1L, trinity, sunday);
    earlier.setSubmittedAt(LocalDateTime.of(2024, 1, 7, 9, 0));
    ServiceInstance later = buildInstance(2L, trinity, sunday);
    later.setSubmittedAt(LocalDateTime.of(2024, 1, 7, 11, 0));

    when(instanceRepository.findByTemplateAndDateRangeWithCelebrants(template, START, END))
        .thenReturn(List.of(later, earlier));
    when(churchService.findAll()).thenReturn(List.of(trinity));
    when(responseRepository.findByServiceInstanceInWithItems(List.of(earlier))).thenReturn(
        List.of(buildResponse(numericalItem, earlier, "100")));

    ConventionReportResponse result = conventionReportService.compute(template, START, END);

    assertThat(result.rows().get(0).values().get(0)).isEqualTo(100.0);
    assertThat(result.rows().get(0).reportCount()).isEqualTo(1);
    assertThat(result.duplicateNotes()).hasSize(1);
    assertThat(result.duplicateNotes().get(0).churchName()).isEqualTo("Trinity");
    assertThat(result.duplicateNotes().get(0).serviceDate()).isEqualTo(sunday);
    assertThat(result.duplicateNotes().get(0).reportCount()).isEqualTo(2);
  }
}
