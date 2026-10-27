package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemResponse;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemType;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.repositories.ServiceInfoItemResponseRepository;
import org.iecr.diocesedashboard.domain.repositories.ServiceInstanceRepository;
import org.iecr.diocesedashboard.webapp.controller.ConventionReportResponse;
import org.iecr.diocesedashboard.webapp.controller.ConventionReportResponse.ChurchRow;
import org.iecr.diocesedashboard.webapp.controller.ConventionReportResponse.DuplicateReportNote;
import org.iecr.diocesedashboard.webapp.controller.ConventionReportResponse.ItemColumn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Computes the Convention Report: for a service template and date range, sums every
 * numerical/money item's responses across all Sundays, per church.
 */
@Service
public class ConventionReportService {

  private final ServiceInstanceRepository instanceRepository;
  private final ServiceInfoItemResponseRepository responseRepository;
  private final ChurchService churchService;

  @Autowired
  public ConventionReportService(ServiceInstanceRepository instanceRepository,
      ServiceInfoItemResponseRepository responseRepository,
      ChurchService churchService) {
    this.instanceRepository = instanceRepository;
    this.responseRepository = responseRepository;
    this.churchService = churchService;
  }

  /**
   * Computes the Convention Report for the given template and date range.
   *
   * @param template  the service template to aggregate data for
   * @param startDate first date (inclusive)
   * @param endDate   last date (inclusive)
   * @return the aggregated convention report
   */
  @Transactional(readOnly = true)
  public ConventionReportResponse compute(ServiceTemplate template,
      LocalDate startDate, LocalDate endDate) {
    List<ServiceInfoItem> items = template.getServiceInfoItems() != null
        ? template.getServiceInfoItems() : List.of();
    List<ServiceInfoItem> numericItems = items.stream()
        .filter(i -> i.getServiceInfoItemType() != ServiceInfoItemType.STRING)
        .toList();

    List<ServiceInstance> sundayInstances =
        instanceRepository.findByTemplateAndDateRangeWithCelebrants(template, startDate, endDate)
            .stream()
            .filter(i -> i.getServiceDate() != null
                && i.getServiceDate().getDayOfWeek() == DayOfWeek.SUNDAY)
            .toList();

    List<DuplicateReportNote> duplicateNotes = new ArrayList<>();
    List<ServiceInstance> selectedInstances = selectFirstReportPerChurchAndDate(
        sundayInstances, duplicateNotes);

    Map<String, Long> reportCountByChurch = selectedInstances.stream()
        .collect(Collectors.groupingBy(i -> i.getChurch().getName(), Collectors.counting()));

    Map<String, Map<Long, Double>> totalsByChurch = sumResponsesByChurchAndItem(selectedInstances);

    List<Church> allChurches = churchService.findAll();
    List<ChurchRow> rows = allChurches.stream()
        .map(church -> buildChurchRow(church, numericItems, totalsByChurch, reportCountByChurch))
        .toList();

    List<ItemColumn> columns = numericItems.stream()
        .map(item -> new ItemColumn(
            item.getId(), item.getTitle(), item.getServiceInfoItemType().name()))
        .toList();

    return new ConventionReportResponse(
        template.getId(), template.getServiceTemplateName(), startDate, endDate,
        columns, rows, duplicateNotes);
  }

  /**
   * Groups instances by church and service date, keeping only the first submitted instance
   * (earliest {@code submittedAt}, falling back to lowest ID) for each church/date pair.
   * Any pair with more than one instance is recorded in {@code duplicateNotes}.
   */
  private List<ServiceInstance> selectFirstReportPerChurchAndDate(
      List<ServiceInstance> instances, List<DuplicateReportNote> duplicateNotes) {
    Map<String, Map<LocalDate, List<ServiceInstance>>> grouped = new TreeMap<>();
    for (ServiceInstance instance : instances) {
      grouped
          .computeIfAbsent(instance.getChurch().getName(), k -> new TreeMap<>())
          .computeIfAbsent(instance.getServiceDate(), k -> new ArrayList<>())
          .add(instance);
    }

    Comparator<ServiceInstance> bySubmittedThenId = Comparator
        .comparing((ServiceInstance si) -> si.getSubmittedAt() != null
            ? si.getSubmittedAt() : LocalDateTime.MAX)
        .thenComparing(ServiceInstance::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    List<ServiceInstance> selected = new ArrayList<>();
    for (Map.Entry<String, Map<LocalDate, List<ServiceInstance>>> churchEntry
        : grouped.entrySet()) {
      for (Map.Entry<LocalDate, List<ServiceInstance>> dateEntry
          : churchEntry.getValue().entrySet()) {
        List<ServiceInstance> group = dateEntry.getValue();
        selected.add(group.stream().min(bySubmittedThenId).orElseThrow());
        if (group.size() > 1) {
          duplicateNotes.add(new DuplicateReportNote(
              churchEntry.getKey(), dateEntry.getKey(), group.size()));
        }
      }
    }
    return selected;
  }

  private Map<String, Map<Long, Double>> sumResponsesByChurchAndItem(
      List<ServiceInstance> selectedInstances) {
    if (selectedInstances.isEmpty()) {
      return Map.of();
    }
    List<ServiceInfoItemResponse> responses =
        responseRepository.findByServiceInstanceInWithItems(selectedInstances);

    Map<String, Map<Long, Double>> totals = new HashMap<>();
    for (ServiceInfoItemResponse response : responses) {
      ServiceInfoItem item = response.getServiceInfoItem();
      if (item.getServiceInfoItemType() == ServiceInfoItemType.STRING) {
        continue;
      }
      String churchName = response.getServiceInstance().getChurch().getName();
      double val = parseDouble(response.getResponseValue());
      totals.computeIfAbsent(churchName, k -> new HashMap<>())
          .merge(item.getId(), val, Double::sum);
    }
    return totals;
  }

  private ChurchRow buildChurchRow(Church church, List<ServiceInfoItem> numericItems,
      Map<String, Map<Long, Double>> totalsByChurch, Map<String, Long> reportCountByChurch) {
    Map<Long, Double> churchTotals = totalsByChurch.getOrDefault(church.getName(), Map.of());
    List<Double> values = numericItems.stream()
        .map(item -> churchTotals.getOrDefault(item.getId(), 0.0))
        .toList();
    int reportCount = reportCountByChurch.getOrDefault(church.getName(), 0L).intValue();
    return new ChurchRow(church.getName(), reportCount, values);
  }

  private static double parseDouble(String value) {
    if (value == null || value.isBlank()) {
      return 0.0;
    }
    try {
      return Double.parseDouble(value.trim());
    } catch (NumberFormatException e) {
      return 0.0;
    }
  }
}
