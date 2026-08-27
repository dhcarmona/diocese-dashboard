package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.Celebrant;
import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ReporterLink;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemResponse;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemType;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.repositories.ReporterLinkRepository;
import org.iecr.diocesedashboard.domain.repositories.ServiceInfoItemResponseRepository;
import org.iecr.diocesedashboard.domain.repositories.ServiceInstanceRepository;
import org.iecr.diocesedashboard.webapp.controller.ChartDrillDownResponse;
import org.iecr.diocesedashboard.webapp.controller.StatisticsResponse;
import org.iecr.diocesedashboard.webapp.controller.StatisticsResponse.AggregatedItem;
import org.iecr.diocesedashboard.webapp.controller.StatisticsResponse.CelebrantStat;
import org.iecr.diocesedashboard.webapp.controller.StatisticsResponse.PendingLink;
import org.iecr.diocesedashboard.webapp.controller.StatisticsResponse.PerChurchPoint;
import org.iecr.diocesedashboard.webapp.controller.StatisticsResponse.TimeSeriesPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Computes aggregate statistics for service instances over a date range. */
@Service
public class StatisticsService {

  private final ServiceInstanceRepository instanceRepository;
  private final ServiceInfoItemResponseRepository responseRepository;
  private final ReporterLinkRepository reporterLinkRepository;
  private final ChurchService churchService;

  @Autowired
  public StatisticsService(ServiceInstanceRepository instanceRepository,
      ServiceInfoItemResponseRepository responseRepository,
      ReporterLinkRepository reporterLinkRepository,
      ChurchService churchService) {
    this.instanceRepository = instanceRepository;
    this.responseRepository = responseRepository;
    this.reporterLinkRepository = reporterLinkRepository;
    this.churchService = churchService;
  }

  /**
   * Computes statistics for a specific church and template over the given date range.
   *
   * @param template  the service template
   * @param church          the church to scope the report to
   * @param startDate       first date (inclusive)
   * @param endDate         last date (inclusive)
   * @param reporterUserId  when non-null, pending links are filtered to this reporter only
   * @return the aggregated statistics response
   */
  @Transactional(readOnly = true)
  public StatisticsResponse computeForChurch(ServiceTemplate template, Church church,
      LocalDate startDate, LocalDate endDate, Long reporterUserId) {
    List<ServiceInstance> instances =
        instanceRepository.findByTemplateAndChurchAndDateRangeWithCelebrants(
            template, church, startDate, endDate);
    List<ReporterLink> links =
        reporterLinkRepository.findByChurchAndServiceTemplate(church, template);
    return build(template, church.getName(), false, startDate, endDate, instances, links,
        reporterUserId);
  }

  /**
   * Computes a global statistics report covering all churches, for the given template and dates.
   *
   * @param template  the service template
   * @param startDate first date (inclusive)
   * @param endDate   last date (inclusive)
   * @return the aggregated statistics response
   */
  @Transactional(readOnly = true)
  public StatisticsResponse computeGlobal(ServiceTemplate template,
      LocalDate startDate, LocalDate endDate) {
    List<ServiceInstance> instances =
        instanceRepository.findByTemplateAndDateRangeWithCelebrants(
            template, startDate, endDate);
    List<Church> allChurches = churchService.findAll();
    List<ReporterLink> links =
        reporterLinkRepository.findByChurchInAndServiceTemplate(allChurches, template);
    return build(template, null, true, startDate, endDate, instances, links, null);
  }

  private StatisticsResponse build(ServiceTemplate template, String churchName, boolean global,
      LocalDate startDate, LocalDate endDate,
      List<ServiceInstance> instances, List<ReporterLink> links, Long reporterUserId) {

    List<CelebrantStat> celebrantStats = computeCelebrantStats(instances);

    List<ServiceInfoItem> items = template.getServiceInfoItems() != null
        ? template.getServiceInfoItems() : List.of();

    List<ServiceInfoItem> numericalItems = items.stream()
        .filter(i -> i.getServiceInfoItemType() == ServiceInfoItemType.NUMERICAL)
        .toList();
    List<ServiceInfoItem> moneyItems = items.stream()
        .filter(i -> i.getServiceInfoItemType() == ServiceInfoItemType.DOLLARS
            || i.getServiceInfoItemType() == ServiceInfoItemType.COLONES)
        .toList();

    List<ServiceInfoItemResponse> allResponses = instances.isEmpty()
        ? List.of()
        : responseRepository.findByServiceInstanceInWithItems(instances);

    Map<Long, Map<LocalDate, Double>> perItemPerDate = aggregateByItemAndDate(allResponses);
    Map<Long, List<PerChurchPoint>> perItemPerChurch =
        global ? aggregateByItemChurchAndDate(allResponses) : Map.of();

    List<AggregatedItem> numAgg =
        buildAggregatedItems(numericalItems, perItemPerDate, perItemPerChurch);
    List<AggregatedItem> moneyAgg =
        buildAggregatedItems(moneyItems, perItemPerDate, perItemPerChurch);

    List<PendingLink> pendingLinks = links.stream()
        .filter(l -> reporterUserId == null || reporterUserId.equals(l.getReporter().getId()))
        .map(l -> new PendingLink(
            l.getToken(),
            l.getReporter().getUsername(),
            l.getReporter().getFullName(),
            l.getChurch().getName(),
            l.getActiveDate()))
        .sorted((a, bb) -> a.activeDate().compareTo(bb.activeDate()))
        .toList();

    return new StatisticsResponse(
        template.getId(),
        template.getServiceTemplateName(),
        churchName,
        global,
        startDate,
        endDate,
        instances.size(),
        celebrantStats,
        numAgg,
        moneyAgg,
        pendingLinks);
  }

  private List<CelebrantStat> computeCelebrantStats(List<ServiceInstance> instances) {
    Map<Long, int[]> countMap = new LinkedHashMap<>();
    Map<Long, String> nameMap = new HashMap<>();

    for (ServiceInstance instance : instances) {
      if (instance.getCelebrants() == null) {
        continue;
      }
      for (Celebrant celebrant : instance.getCelebrants()) {
        countMap.computeIfAbsent(celebrant.getId(), k -> new int[]{0})[0]++;
        nameMap.putIfAbsent(celebrant.getId(), celebrant.getName());
      }
    }

    return countMap.entrySet().stream()
        .map(e -> new CelebrantStat(e.getKey(), nameMap.get(e.getKey()), e.getValue()[0]))
        .sorted((a, bb) -> Integer.compare(bb.serviceCount(), a.serviceCount()))
        .toList();
  }

  private Map<Long, Map<LocalDate, Double>> aggregateByItemAndDate(
      List<ServiceInfoItemResponse> allResponses) {
    if (allResponses.isEmpty()) {
      return Map.of();
    }
    Map<Long, Map<LocalDate, Double>> result = new HashMap<>();

    for (ServiceInfoItemResponse response : allResponses) {
      ServiceInfoItem item = response.getServiceInfoItem();
      if (item.getServiceInfoItemType() == ServiceInfoItemType.STRING) {
        continue;
      }
      LocalDate date = response.getServiceInstance().getServiceDate();
      double val = parseDouble(response.getResponseValue());
      result.computeIfAbsent(item.getId(), k -> new TreeMap<>())
          .merge(date, val, Double::sum);
    }

    return result;
  }

  private Map<Long, List<PerChurchPoint>> aggregateByItemChurchAndDate(
      List<ServiceInfoItemResponse> allResponses) {
    if (allResponses.isEmpty()) {
      return Map.of();
    }
    // itemId -> (church+date key) -> summed value; use a TreeMap to keep dates sorted
    record ChurchDateKey(String churchName, LocalDate date) implements Comparable<ChurchDateKey> {
      @Override
      public int compareTo(ChurchDateKey other) {
        int dateCompare = this.date.compareTo(other.date);
        return dateCompare != 0 ? dateCompare : this.churchName.compareTo(other.churchName);
      }
    }

    Map<Long, Map<ChurchDateKey, Double>> raw = new HashMap<>();
    for (ServiceInfoItemResponse response : allResponses) {
      ServiceInfoItem item = response.getServiceInfoItem();
      if (item.getServiceInfoItemType() == ServiceInfoItemType.STRING) {
        continue;
      }
      String churchName = response.getServiceInstance().getChurch().getName();
      LocalDate date = response.getServiceInstance().getServiceDate();
      double val = parseDouble(response.getResponseValue());
      raw.computeIfAbsent(item.getId(), k -> new TreeMap<>())
          .merge(new ChurchDateKey(churchName, date), val, Double::sum);
    }

    Map<Long, List<PerChurchPoint>> result = new HashMap<>();
    for (Map.Entry<Long, Map<ChurchDateKey, Double>> entry : raw.entrySet()) {
      List<PerChurchPoint> points = entry.getValue().entrySet().stream()
          .map(e -> new PerChurchPoint(e.getKey().churchName(), e.getKey().date(), e.getValue()))
          .toList();
      result.put(entry.getKey(), points);
    }
    return result;
  }

  private List<AggregatedItem> buildAggregatedItems(List<ServiceInfoItem> items,
      Map<Long, Map<LocalDate, Double>> perItemPerDate,
      Map<Long, List<PerChurchPoint>> perItemPerChurch) {
    List<AggregatedItem> result = new ArrayList<>();
    for (ServiceInfoItem item : items) {
      Map<LocalDate, Double> byDate = perItemPerDate.getOrDefault(item.getId(), Map.of());
      double total = byDate.values().stream().mapToDouble(Double::doubleValue).sum();
      List<TimeSeriesPoint> series = byDate.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .map(e -> new TimeSeriesPoint(e.getKey(), e.getValue()))
          .toList();
      List<PerChurchPoint> perChurch =
          perItemPerChurch.getOrDefault(item.getId(), List.of());
      result.add(new AggregatedItem(
          item.getId(),
          item.getTitle(),
          item.getServiceInfoItemType().name(),
          total,
          series,
          perChurch));
    }
    return result;
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

  /**
   * Returns drill-down detail for a single bar in a statistics chart.
   * Returns the service instances on the given date that have a non-zero value for the item,
   * sorted by value descending.
   *
   * @param template  the service template
   * @param item      the service info item (must belong to template)
   * @param date      the exact service date
   * @param church    optional church filter; null means all churches
   * @return the drill-down detail response
   */
  @Transactional(readOnly = true)
  public ChartDrillDownResponse computeDrillDown(ServiceTemplate template, ServiceInfoItem item,
      LocalDate date, Church church) {
    List<ServiceInstance> instances;
    if (church != null) {
      instances = instanceRepository.findByTemplateAndChurchAndDateRangeWithCelebrants(
          template, church, date, date);
    } else {
      instances = instanceRepository.findByTemplateAndDateRangeWithCelebrants(
          template, date, date);
    }

    if (instances.isEmpty()) {
      return new ChartDrillDownResponse(item.getId(), item.getTitle(),
          item.getServiceInfoItemType().name(), date, List.of());
    }

    List<ServiceInfoItemResponse> responses =
        responseRepository.findByServiceInstancesAndItem(instances, item);

    List<ChartDrillDownResponse.DrillDownRow> rows = responses.stream()
        .map(r -> {
          double value = parseDouble(r.getResponseValue());
          DashboardUser submittedBy = r.getServiceInstance().getSubmittedBy();
          String username = submittedBy != null ? submittedBy.getUsername() : null;
          String fullName = submittedBy != null ? submittedBy.getFullName() : null;
          return new ChartDrillDownResponse.DrillDownRow(
              r.getServiceInstance().getId(),
              r.getServiceInstance().getChurch().getName(),
              username,
              fullName,
              value);
        })
        .filter(row -> row.value() > 0)
        .sorted((a, bb) -> Double.compare(bb.value(), a.value()))
        .toList();

    return new ChartDrillDownResponse(item.getId(), item.getTitle(),
        item.getServiceInfoItemType().name(), date, rows);
  }
}
