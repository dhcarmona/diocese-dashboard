package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.SectionHeader;

/**
 * Public DTO for a {@link SectionHeader}, safe to expose without authentication.
 * Contains only the fields the report submission form needs to render a section separator.
 */
public record SectionHeaderSummary(
Long id,
String title,
Integer sortOrder) {

  /**
   * Creates a summary DTO from a {@link SectionHeader} entity.
   *
   * @param header the source entity
   * @return the summary DTO
   */
  public static SectionHeaderSummary from(SectionHeader header) {
    return new SectionHeaderSummary(
        header.getId(),
        header.getTitle(),
        header.getSortOrder());
  }
}
