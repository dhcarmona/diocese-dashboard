package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.Celebrant;
import org.iecr.diocesedashboard.domain.objects.ReporterLink;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;

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
LocalDate activeDate,
List<ServiceInfoItem> serviceInfoItems,
List<Celebrant> celebrants) {

  /**
   * Creates a public response DTO from a {@link ReporterLink} and associated form data.
   *
   * @param link        the persisted reporter link
   * @param infoItems   the ordered service info items for the link's template
   * @param celebrants  all available celebrants
   * @return the public response payload
   */
  public static ReporterLinkPublicResponse from(
      ReporterLink link,
      List<ServiceInfoItem> infoItems,
      List<Celebrant> celebrants) {
    return new ReporterLinkPublicResponse(
        link.getId(),
        link.getToken(),
        link.getChurch().getName(),
        link.getServiceTemplate().getId(),
        link.getServiceTemplate().getServiceTemplateName(),
        link.getActiveDate(),
        infoItems,
        celebrants);
  }
}
