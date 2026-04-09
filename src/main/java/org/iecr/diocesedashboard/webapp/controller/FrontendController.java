package org.iecr.diocesedashboard.webapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards known React SPA routes to index.html, enabling client-side routing
 * without intercepting static asset requests.
 */
@Controller
public class FrontendController {

  /**
   * Forwards supported SPA routes to index.html.
   *
   * @return a forward directive to index.html
   */
  @GetMapping(value = {
      "/login",
      "/reports/new",
      "/submit/service-templates/{templateId}",
      "/service-templates/manage",
      "/users/manage",
      "/celebrants/manage",
      "/churches/manage",
      "/reporter-links/manage",
      "/statistics",
      "/statistics/{templateId}",
      "/statistics/{templateId}/report",
      "/whatsapp-logs",
      "/reports/view/individual/{church}/{template}",
      "/reports/view/individual/{church}/{template}/{reportId}"
  })
  public String forwardToIndex() {
    return "forward:/index.html";
  }
}
