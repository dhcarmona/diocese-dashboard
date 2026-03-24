package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.webapp.DashboardUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for authenticated-session helper endpoints used by the SPA. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  /**
   * Returns the current CSRF token so the SPA can send it on state-changing requests.
   *
   * @param csrfToken the current CSRF token
   * @return the token payload
   */
  @GetMapping("/csrf")
  public ResponseEntity<CsrfTokenResponse> getCsrfToken(CsrfToken csrfToken) {
    return ResponseEntity.ok(
        new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken()));
  }

  /**
   * Returns the authenticated user in a frontend-safe shape.
   *
   * @param authentication the current Spring Security authentication
   * @return the authenticated user payload
   */
  @GetMapping("/me")
  public ResponseEntity<AuthenticatedUserResponse> getAuthenticatedUser(
      Authentication authentication) {
    DashboardUser user =
        ((DashboardUserDetails) authentication.getPrincipal()).getDashboardUser();
    return ResponseEntity.ok(AuthenticatedUserResponse.from(user));
  }
}
