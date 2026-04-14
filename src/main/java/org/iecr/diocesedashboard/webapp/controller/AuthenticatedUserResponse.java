package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.UserRole;

import java.util.List;

/** Safe authenticated-user payload for the SPA. */
public record AuthenticatedUserResponse(
Long id,
String username,
UserRole role,
String preferredLanguage,
List<String> assignedChurchNames) {

  /**
   * Creates a serialized response payload from the authenticated dashboard user.
   *
   * @param user the authenticated user entity
   * @return response payload without password data
   */
  public static AuthenticatedUserResponse from(DashboardUser user) {
    return new AuthenticatedUserResponse(
        user.getId(),
        user.getUsername(),
        user.getRole(),
        user.getPreferredLanguage(),
        user.getAssignedChurches().stream()
            .map(church -> church.getName())
            .sorted()
            .toList());
  }
}
