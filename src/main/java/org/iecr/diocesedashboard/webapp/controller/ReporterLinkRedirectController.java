package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.service.ReporterLinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Publicly accessible short-URL redirect controller.
 * Validates that a reporter link token exists, then redirects the browser to the
 * login page with a {@code redirect} parameter pointing back to this URL.
 * After the UI is built, the login page should honour that parameter and send
 * the authenticated reporter directly to the service-report form for their token.
 */
@RestController
@RequestMapping("/r")
public class ReporterLinkRedirectController {

  private final ReporterLinkService reporterLinkService;

  @Autowired
  public ReporterLinkRedirectController(ReporterLinkService reporterLinkService) {
    this.reporterLinkService = reporterLinkService;
  }

  /**
   * Validates the given token and redirects to {@code /login?redirect=/r/{token}}.
   * Returns 404 if the token does not correspond to any known reporter link,
   * so stale or invalid links fail fast before the user even reaches the login page.
   *
   * @param token the reporter link token embedded in the short URL
   * @return 302 redirect to the login page, or 404 if the token is unknown
   */
  @GetMapping("/{token}")
  public ResponseEntity<Void> redirect(@PathVariable String token) {
    if (!reporterLinkService.existsByToken(token)) {
      return ResponseEntity.notFound().build();
    }
    URI loginRedirect = UriComponentsBuilder.fromPath("/login")
        .queryParam("redirect", UriComponentsBuilder.fromPath("/r/{token}")
            .buildAndExpand(token)
            .encode()
            .toUriString())
        .build()
        .encode()
        .toUri();
    HttpHeaders headers = new HttpHeaders();
    headers.setLocation(loginRedirect);
    return new ResponseEntity<>(headers, HttpStatus.FOUND);
  }
}
