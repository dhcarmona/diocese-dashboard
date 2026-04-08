package org.iecr.diocesedashboard.webapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.iecr.diocesedashboard.domain.objects.WhatsAppMessageLog;
import org.iecr.diocesedashboard.domain.repositories.WhatsAppMessageLogRepository;
import org.iecr.diocesedashboard.webapp.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

@WebMvcTest(WhatsAppMessageLogController.class)
@Import(SecurityConfig.class)
class WhatsAppMessageLogControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private WhatsAppMessageLogRepository messageLogRepository;

  @MockBean
  private UserDetailsService userDetailsService;

  private WhatsAppMessageLog buildLog(
      long id, String username, String body, boolean otp) {
    WhatsAppMessageLog log = new WhatsAppMessageLog();
    log.setId(id);
    log.setRecipientUsername(username);
    log.setBody(body);
    log.setOtp(otp);
    log.setSentAt(Instant.parse("2024-01-15T10:00:00Z"));
    return log;
  }

  // --- GET /api/whatsapp-logs ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAll_asAdmin_returns200WithPageContent() throws Exception {
    Page<WhatsAppMessageLog> mockPage = new PageImpl<>(
        List.of(
            buildLog(1L, "alice", "Hello Alice", false),
            buildLog(2L, "bob", null, true)));
    when(messageLogRepository.findAll(any(Pageable.class))).thenReturn(mockPage);

    mockMvc.perform(get("/api/whatsapp-logs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].recipientUsername").value("alice"))
        .andExpect(jsonPath("$.content[0].body").value("Hello Alice"))
        .andExpect(jsonPath("$.content[0].otp").value(false))
        .andExpect(jsonPath("$.content[1].recipientUsername").value("bob"))
        .andExpect(jsonPath("$.content[1].body").isEmpty())
        .andExpect(jsonPath("$.content[1].otp").value(true))
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAll_respectsPageAndSizeParams() throws Exception {
    Page<WhatsAppMessageLog> mockPage = new PageImpl<>(
        List.of(buildLog(3L, "charlie", "Hi", false)));
    when(messageLogRepository.findAll(any(Pageable.class))).thenReturn(mockPage);

    mockMvc.perform(get("/api/whatsapp-logs").param("page", "1").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].recipientUsername").value("charlie"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAll_emptyLog_returns200WithEmptyContent() throws Exception {
    when(messageLogRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

    mockMvc.perform(get("/api/whatsapp-logs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0))
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  @WithMockUser(roles = "REPORTER")
  void getAll_asReporter_returns403() throws Exception {
    mockMvc.perform(get("/api/whatsapp-logs"))
        .andExpect(status().isForbidden());
  }

  @Test
  void getAll_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/whatsapp-logs"))
        .andExpect(status().isUnauthorized());
  }
}
