package org.iecr.diocesedashboard.webapp.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.iecr.diocesedashboard.webapp.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/** Tests for {@link ClientErrorController}. */
@WebMvcTest(ClientErrorController.class)
@Import(SecurityConfig.class)
class ClientErrorControllerTest {

  private static final String URL = "/api/client-errors";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserDetailsService userDetailsService;

  @Test
  void reportError_validPayload_returns204() throws Exception {
    mockMvc.perform(post(URL).with(csrf())
        .contentType("application/json")
        .content("""
            {"message":"TypeError: Cannot read properties of null",
             "stack":"at handleClick (main.js:12)",
             "url":"/r/abc123",
             "userAgent":"Mozilla/5.0 (Linux; Android 14) Chrome/124"}
            """))
        .andExpect(status().isNoContent());
  }

  @Test
  void reportError_nullStackAndOptionalFields_returns204() throws Exception {
    mockMvc.perform(post(URL).with(csrf())
        .contentType("application/json")
        .content("{\"message\":\"ReferenceError: x is not defined\",\"stack\":null,"
            + "\"url\":null,\"userAgent\":null}"))
        .andExpect(status().isNoContent());
  }

  @Test
  void reportError_blankMessage_returns400() throws Exception {
    mockMvc.perform(post(URL).with(csrf())
        .contentType("application/json")
        .content("{\"message\":\"\",\"stack\":null,\"url\":null,\"userAgent\":null}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void reportError_messageTooLong_returns400() throws Exception {
    String longMessage = "x".repeat(501);

    mockMvc.perform(post(URL).with(csrf())
        .contentType("application/json")
        .content("{\"message\":\"" + longMessage + "\",\"stack\":null,"
            + "\"url\":null,\"userAgent\":null}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void reportError_stackTooLong_returns400() throws Exception {
    String longStack = "a".repeat(5001);

    mockMvc.perform(post(URL).with(csrf())
        .contentType("application/json")
        .content("{\"message\":\"err\",\"stack\":\"" + longStack + "\","
            + "\"url\":null,\"userAgent\":null}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void reportError_missingCsrfToken_returns403() throws Exception {
    mockMvc.perform(post(URL)
        .contentType("application/json")
        .content("{\"message\":\"err\",\"stack\":null,\"url\":null,\"userAgent\":null}"))
        .andExpect(status().isForbidden());
  }
}
