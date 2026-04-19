package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplateType;

/**
 * Lightweight read-only projection of a {@link ServiceTemplate}, containing only the fields
 * needed for list/selection views. Avoids loading lazy collections on the entity.
 *
 * @param id                  the template ID
 * @param serviceTemplateName the human-readable template name
 * @param templateType        the template type, or {@code null} if not set
 * @param linkOnly            whether the template is accessible via reporter links only
 * @param bannerUrl           optional URL for the template's banner image
 */
public record ServiceTemplateSummaryResponse(
Long id,
String serviceTemplateName,
ServiceTemplateType templateType,
boolean linkOnly,
String bannerUrl) {

  /** Creates a summary response from the given {@link ServiceTemplate}. */
  public static ServiceTemplateSummaryResponse from(ServiceTemplate template) {
    return new ServiceTemplateSummaryResponse(
        template.getId(),
        template.getServiceTemplateName(),
        template.getTemplateType(),
        template.isLinkOnly(),
        template.getBannerUrl());
  }
}
