package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemType;

/**
 * Public DTO for a {@link ServiceInfoItem}, safe to expose without authentication.
 * Contains only the fields the report submission form needs.
 */
public record ServiceInfoItemSummary(
Long id,
String title,
String description,
boolean required,
ServiceInfoItemType serviceInfoItemType,
Integer sortOrder) {

  /**
   * Creates a summary DTO from a {@link ServiceInfoItem} entity.
   *
   * @param item the source entity
   * @return the summary DTO
   */
  public static ServiceInfoItemSummary from(ServiceInfoItem item) {
    return new ServiceInfoItemSummary(
        item.getId(),
        item.getTitle(),
        item.getDescription(),
        Boolean.TRUE.equals(item.getRequired()),
        item.getServiceInfoItemType(),
        item.getSortOrder());
  }
}
