package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.service.ChurchService;
import org.iecr.diocesedashboard.service.ServiceInfoItemService;
import org.iecr.diocesedashboard.service.ServiceTemplateService;
import org.iecr.diocesedashboard.service.StatisticsService;
import org.iecr.diocesedashboard.webapp.DashboardUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/** REST controller that exposes aggregate statistics for service instances. */
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

  private final StatisticsService statisticsService;
  private final ServiceTemplateService serviceTemplateService;
  private final ChurchService churchService;
  private final ServiceInfoItemService serviceInfoItemService;

  @Autowired
  public StatisticsController(StatisticsService statisticsService,
      ServiceTemplateService serviceTemplateService,
      ChurchService churchService,
      ServiceInfoItemService serviceInfoItemService) {
    this.statisticsService = statisticsService;
    this.serviceTemplateService = serviceTemplateService;
    this.churchService = churchService;
    this.serviceInfoItemService = serviceInfoItemService;
  }

  /**
   * Returns all service templates available for statistics, including link-only templates.
   * This endpoint is available to all authenticated users since reporters are allowed to view
   * statistics for link-only templates even though they cannot submit them directly.
   * Returns a lightweight summary to avoid loading lazy collections on each template entity.
   *
   * @return list of all service template summaries
   */
  @GetMapping("/templates")
  public List<ServiceTemplateSummaryResponse> getTemplatesForStatistics() {
    return serviceTemplateService.findAll().stream()
        .map(ServiceTemplateSummaryResponse::from)
        .toList();
  }

  /**
   * Returns an aggregated statistics report for the given template, optional church, and date
   * range. When {@code churchName} is omitted, a global report over all churches is returned
   * (ADMIN only). Reporter users may only query churches they are assigned to.
   *
   * @param templateId the template to aggregate data for
   * @param churchName the church name to scope the report; omit for a global report (ADMIN only)
   * @param startDate  report start date (inclusive)
   * @param endDate    report end date (inclusive)
   * @param auth       the authenticated principal
   * @return the aggregated statistics response
   */
  @GetMapping
  public StatisticsResponse getStatistics(
      @RequestParam Long templateId,
      @RequestParam(required = false) String churchName,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      Authentication auth) {

    DashboardUser user = ((DashboardUserDetails) auth.getPrincipal()).getDashboardUser();

    ServiceTemplate template = serviceTemplateService.findById(templateId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Template not found: " + templateId));

    if (churchName == null) {
      if (user.getRole() != UserRole.ADMIN) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Global reports are only available to ADMIN users");
      }
      return statisticsService.computeGlobal(template, startDate, endDate);
    }

    Church church = churchService.findById(churchName)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Church not found: " + churchName));

    if (user.getRole() == UserRole.REPORTER && !user.isAssignedToChurch(church)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "Reporter is not assigned to church: " + churchName);
    }

    Long reporterUserId = user.getRole() == UserRole.REPORTER ? user.getId() : null;
    return statisticsService.computeForChurch(template, church, startDate, endDate, reporterUserId);
  }

  /**
   * Returns drill-down detail for a specific bar in a statistics chart.
   * Shows which service instances contributed to the aggregate value for a given item and date.
   * ADMIN only.
   *
   * @param templateId the service template
   * @param itemId     the service info item
   * @param date       the exact service date of the bar
   * @param churchName optional church filter; omit for a global drill-down
   * @return the drill-down detail response
   */
  @GetMapping("/drill-down")
  public ChartDrillDownResponse getDrillDown(
      @RequestParam Long templateId,
      @RequestParam Long itemId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) String churchName) {

    ServiceTemplate template = serviceTemplateService.findById(templateId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Template not found: " + templateId));

    ServiceInfoItem item = serviceInfoItemService.findById(itemId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Item not found: " + itemId));

    if (item.getServiceTemplate() == null
        || !item.getServiceTemplate().getId().equals(templateId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Item does not belong to the given template");
    }

    Church church = null;
    if (churchName != null) {
      church = churchService.findById(churchName)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
              "Church not found: " + churchName));
    }

    return statisticsService.computeDrillDown(template, item, date, church);
  }
}
