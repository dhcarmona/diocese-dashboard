package org.iecr.diocesedashboard.webapp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.iecr.diocesedashboard.service.ReporterLinkService;
import org.iecr.diocesedashboard.webapp.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReporterLinkRedirectController.class)
@Import(SecurityConfig.class)
class ReporterLinkRedirectControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ReporterLinkRedirectController controller;

  @MockBean
  private ReporterLinkService reporterLinkService;

  @MockBean
  private UserDetailsService userDetailsService;

  private static final String TOKEN = "abc-123-token";

  @Test
  void redirect_validToken_returns302ToLoginWithRedirectParam() throws Exception {
    when(reporterLinkService.existsByToken(TOKEN)).thenReturn(true);

    mockMvc.perform(get("/r/" + TOKEN))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/login?redirect=/r/" + TOKEN));
  }

  @Test
  void redirect_reservedCharactersInToken_areEncodedInRedirectTarget() throws Exception {
    String reservedToken = "abc?next=/foo&admin=true";
    when(reporterLinkService.existsByToken(reservedToken)).thenReturn(true);

    var response = controller.redirect(reservedToken);
    String location = response.getHeaders().getLocation().toString();

    assertEquals(302, response.getStatusCode().value());
    assertNotNull(response.getHeaders().getLocation());
    assertTrue(location.startsWith("/login?redirect=/r/abc"));
    assertTrue(location.contains("%3F") || location.contains("%253F"));
    assertTrue(location.contains("%26") || location.contains("%2526"));
    assertFalse(location.contains("/r/abc?next="));
    assertFalse(location.contains("&admin=true"));
  }

  @Test
  void redirect_unknownToken_returns404() throws Exception {
    when(reporterLinkService.existsByToken("bad-token")).thenReturn(false);

    mockMvc.perform(get("/r/bad-token"))
        .andExpect(status().isNotFound());
  }

  @Test
  void redirect_noCredentialsRequired_publiclyAccessible() throws Exception {
    when(reporterLinkService.existsByToken(TOKEN)).thenReturn(true);

    // No auth headers — should still get 302, not 401
    mockMvc.perform(get("/r/" + TOKEN))
        .andExpect(status().isFound());
  }
}
