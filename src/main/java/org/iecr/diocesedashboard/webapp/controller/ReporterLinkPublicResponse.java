package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.ReporterLink;

import java.time.LocalDate;
import java.util.List;

/**
 * Public API response for a reporter link.
 * Bundles all data needed to render the report submission form
 * without requiring the caller to be authenticated.
 */
public record ReporterLinkPublicResponse(
Long id,
String token,
String churchName,
Long serviceTemplateId,
String serviceTemplateName,
String bannerUrl,
LocalDate activeDate,
List<ServiceInfoItemSummary> serviceInfoItems,
List<CelebrantSummary> celebrants) {

  /**
   * Creates a public response DTO from a {@link ReporterLink} and associated form data.
   *
   * @param link        the persisted reporter link
   * @param bannerUrl   the internal banner URL for the link's service template
   * @param infoItems   the ordered service info item summaries for the link's template
   * @param celebrants  all available celebrant summaries
   * @return the public response payload
   */
  public static ReporterLinkPublicResponse from(
      ReporterLink link,
      String bannerUrl,
      List<ServiceInfoItemSummary> infoItems,
      List<CelebrantSummary> celebrants) {
    return new ReporterLinkPublicResponse(
        link.getId(),
        link.getToken(),
        link.getChurch().getName(),
        link.getServiceTemplate().getId(),
        link.getServiceTemplate().getServiceTemplateName(),
        bannerUrl,
        link.getActiveDate(),
        infoItems,
        celebrants);
  }
}
