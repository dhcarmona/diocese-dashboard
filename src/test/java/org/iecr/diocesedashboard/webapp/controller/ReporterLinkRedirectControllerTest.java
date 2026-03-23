package org.iecr.diocesedashboard.webapp.controller;

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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReporterLinkRedirectController.class)
@Import(SecurityConfig.class)
class ReporterLinkRedirectControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ReporterLinkService reporterLinkService;

  private static final String TOKEN = "abc-123-token";

  @Test
  void redirect_validToken_returns302ToLoginWithRedirectParam() throws Exception {
    when(reporterLinkService.existsByToken(TOKEN)).thenReturn(true);

    mockMvc.perform(get("/r/" + TOKEN))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/login?redirect=/r/" + TOKEN));
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
