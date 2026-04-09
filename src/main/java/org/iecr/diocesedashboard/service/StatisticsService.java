package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.Celebrant;
import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.ReporterLink;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemResponse;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemType;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.repositories.ReporterLinkRepository;
import org.iecr.diocesedashboard.domain.repositories.ServiceInfoItemResponseRepository;
import org.iecr.diocesedashboard.domain.repositories.ServiceInstanceRepository;
import org.iecr.diocesedashboard.webapp.controller.StatisticsResponse;
import org.iecr.diocesedashboard.webapp.controller.StatisticsResponse.AggregatedItem;
import org.iecr.diocesedashboard.webapp.controller.StatisticsResponse.CelebrantStat;
import org.iecr.diocesedashboard.webapp.controller.StatisticsResponse.PendingLink;
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
   * @param church    the church to scope the report to
   * @param startDate first date (inclusive)
   * @param endDate   last date (inclusive)
   * @return the aggregated statistics response
   */
  @Transactional(readOnly = true)
  public StatisticsResponse computeForChurch(ServiceTemplate template, Church church,
      LocalDate startDate, LocalDate endDate) {
    List<ServiceInstance> instances =
        instanceRepository.findByServiceTemplateAndChurchAndServiceDateBetween(
            template, church, startDate, endDate);
    List<ReporterLink> links =
        reporterLinkRepository.findByChurchAndServiceTemplate(church, template);
    return build(template, church.getName(), false, startDate, endDate, instances, links);
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
        instanceRepository.findByServiceTemplateAndServiceDateBetween(
            template, startDate, endDate);
    List<Church> allChurches = churchService.findAll();
    List<ReporterLink> links =
        reporterLinkRepository.findByChurchInAndServiceTemplate(allChurches, template);
    return build(template, null, true, startDate, endDate, instances, links);
  }

  private StatisticsResponse build(ServiceTemplate template, String churchName, boolean global,
      LocalDate startDate, LocalDate endDate,
      List<ServiceInstance> instances, List<ReporterLink> links) {

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

    Map<Long, Map<LocalDate, Double>> perItemPerDate = aggregateByItemAndDate(instances);

    List<AggregatedItem> numAgg = buildAggregatedItems(numericalItems, perItemPerDate);
    List<AggregatedItem> moneyAgg = buildAggregatedItems(moneyItems, perItemPerDate);

    List<PendingLink> pendingLinks = links.stream()
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
      List<ServiceInstance> instances) {
    Map<Long, Map<LocalDate, Double>> result = new HashMap<>();

    for (ServiceInstance instance : instances) {
      LocalDate date = instance.getServiceDate();
      List<ServiceInfoItemResponse> responses = responseRepository.findByServiceInstance(instance);
      for (ServiceInfoItemResponse response : responses) {
        ServiceInfoItem item = response.getServiceInfoItem();
        ServiceInfoItemType type = item.getServiceInfoItemType();
        if (type == ServiceInfoItemType.STRING) {
          continue;
        }
        double val = parseDouble(response.getResponseValue());
        result.computeIfAbsent(item.getId(), k -> new TreeMap<>())
            .merge(date, val, Double::sum);
      }
    }

    return result;
  }

  private List<AggregatedItem> buildAggregatedItems(List<ServiceInfoItem> items,
      Map<Long, Map<LocalDate, Double>> perItemPerDate) {
    List<AggregatedItem> result = new ArrayList<>();
    for (ServiceInfoItem item : items) {
      Map<LocalDate, Double> byDate = perItemPerDate.getOrDefault(item.getId(), Map.of());
      double total = byDate.values().stream().mapToDouble(Double::doubleValue).sum();
      List<TimeSeriesPoint> series = byDate.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .map(e -> new TimeSeriesPoint(e.getKey(), e.getValue()))
          .toList();
      result.add(new AggregatedItem(
          item.getId(),
          item.getTitle(),
          item.getServiceInfoItemType().name(),
          total,
          series));
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
}
