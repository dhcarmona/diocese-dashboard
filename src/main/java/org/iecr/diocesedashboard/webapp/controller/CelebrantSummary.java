package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.Celebrant;

/**
 * Public DTO for a {@link Celebrant}, safe to expose without authentication.
 * Contains only the fields the report submission form needs.
 */
public record CelebrantSummary(Long id, String name, String portraitUrl) {

  /**
   * Creates a summary DTO from a {@link Celebrant} entity.
   *
   * @param celebrant the source entity
   * @return the summary DTO
   */
  public static CelebrantSummary from(Celebrant celebrant) {
    return new CelebrantSummary(
        celebrant.getId(),
        celebrant.getName(),
        celebrant.getPortraitUrl());
  }
}
