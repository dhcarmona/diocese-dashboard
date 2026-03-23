package org.iecr.diocesedashboard.webapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards all non-API, non-static GET requests to the React SPA's index.html,
 * enabling client-side routing via React Router.
 */
@Controller
public class FrontendController {

  /**
   * Catches any path segment without a file extension and forwards to index.html.
   * Static assets (e.g. /assets/main.js) are excluded because they contain a dot.
   *
   * @return a forward directive to index.html
   */
  @GetMapping(value = {"/{path:[^\\.]*}", "/{path:[^\\.]*}/**"})
  public String forwardToIndex() {
    return "forward:/index.html";
  }
}
