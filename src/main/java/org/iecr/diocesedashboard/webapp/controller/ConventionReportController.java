package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.service.ConventionReportService;
import org.iecr.diocesedashboard.service.ServiceTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller that exposes the Convention Report, an ADMIN-only aggregate of numerical
 * and money item totals per church, across all Sundays in a date range.
 */
@RestController
@RequestMapping("/api/convention-report")
public class ConventionReportController {

  private final ConventionReportService conventionReportService;
  private final ServiceTemplateService serviceTemplateService;

  @Autowired
  public ConventionReportController(ConventionReportService conventionReportService,
      ServiceTemplateService serviceTemplateService) {
    this.conventionReportService = conventionReportService;
    this.serviceTemplateService = serviceTemplateService;
  }

  /**
   * Returns all service templates available for the Convention Report.
   *
   * @return list of all service template summaries
   */
  @GetMapping("/templates")
  public List<ServiceTemplateSummaryResponse> getTemplates() {
    return serviceTemplateService.findAll().stream()
        .map(ServiceTemplateSummaryResponse::from)
        .toList();
  }

  /**
   * Returns the Convention Report for the given template and date range, covering all churches.
   *
   * @param templateId the template to aggregate data for
   * @param startDate  report start date (inclusive)
   * @param endDate    report end date (inclusive)
   * @return the aggregated convention report
   */
  @GetMapping
  public ConventionReportResponse getConventionReport(
      @RequestParam Long templateId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

    if (endDate.isBefore(startDate)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "endDate must not be before startDate");
    }

    ServiceTemplate template = serviceTemplateService.findById(templateId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Template not found: " + templateId));

    return conventionReportService.compute(template, startDate, endDate);
  }
}
