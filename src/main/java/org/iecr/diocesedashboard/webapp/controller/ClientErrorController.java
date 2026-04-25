package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives client-side JavaScript errors reported by the browser
 * and emits them to the server log for visibility in Heroku log drains.
 */
@RestController
@RequestMapping("/api/client-errors")
public class ClientErrorController {

  private static final Logger log = LoggerFactory.getLogger(ClientErrorController.class);

  /**
   * Logs a client-side JavaScript error.
   * The endpoint is open to unauthenticated browsers but CSRF-protected, ensuring that
   * only browsers with a valid session (i.e., users who have loaded the app) can submit reports.
   *
   * @param request the error details captured by the browser
   */
  @PostMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void reportError(@RequestBody @Valid ClientErrorRequest request) {
    log.warn("Client-side error [url={}] [ua={}]: {} | Stack: {}",
        request.url(), request.userAgent(), request.message(), request.stack());
  }
}
