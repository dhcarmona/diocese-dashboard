package org.iecr.diocesedashboard.webapp.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.iecr.diocesedashboard.service.PortraitService;
import org.iecr.diocesedashboard.webapp.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

@WebMvcTest(PortraitController.class)
@Import(SecurityConfig.class)
class PortraitControllerTest {

  private static final byte[] SVG_BYTES = "<svg>Ana</svg>".getBytes(StandardCharsets.UTF_8);
  private static final byte[] PNG_BYTES = new byte[]{1, 2, 3};

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private PortraitService portraitService;

  @MockBean
  private UserDetailsService userDetailsService;

  @Test
  @WithMockUser(roles = "ADMIN")
  void getCelebrantPortrait_returnsImageBytesAndCachingHeaders() throws Exception {
    when(portraitService.resolveCelebrantPortrait("Ana Perez"))
        .thenReturn(new PortraitService.PortraitAsset(
            MediaType.valueOf("image/svg+xml"),
            SVG_BYTES));

    mockMvc.perform(get("/api/portraits/celebrants").param("name", "Ana Perez"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/svg+xml"))
        .andExpect(header().string("Cache-Control", "max-age=3600, private"))
        .andExpect(content().bytes(SVG_BYTES));
  }

  @Test
  @WithMockUser(roles = "REPORTER")
  void getChurchPortrait_reporterCanAccessPortraits() throws Exception {
    when(portraitService.resolveChurchPortrait("Cathedral"))
        .thenReturn(new PortraitService.PortraitAsset(
            MediaType.IMAGE_PNG,
            PNG_BYTES));

    mockMvc.perform(get("/api/portraits/churches").param("name", "Cathedral"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG))
        .andExpect(content().bytes(PNG_BYTES));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getChurchPortrait_placeholderResponsePassesThrough() throws Exception {
    byte[] placeholderBytes = "<svg>Church Placeholder</svg>".getBytes(StandardCharsets.UTF_8);
    when(portraitService.resolveChurchPortrait("Missing Church"))
        .thenReturn(new PortraitService.PortraitAsset(
            MediaType.valueOf("image/svg+xml"),
            placeholderBytes));

    mockMvc.perform(get("/api/portraits/churches").param("name", "Missing Church"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/svg+xml"))
        .andExpect(content().bytes(placeholderBytes));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getCelebrantPortrait_acceptsAccentedNamesThroughHttpLayer() throws Exception {
    when(portraitService.resolveCelebrantPortrait("Ána Pérez"))
        .thenReturn(new PortraitService.PortraitAsset(
            MediaType.valueOf("image/svg+xml"),
            SVG_BYTES));

    mockMvc.perform(get("/api/portraits/celebrants").param("name", "Ána Pérez"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/svg+xml"))
        .andExpect(content().bytes(SVG_BYTES));
  }

  @Test
  @WithMockUser(roles = "USER")
  void getCelebrantPortrait_wrongRoleReturns403() throws Exception {
    mockMvc.perform(get("/api/portraits/celebrants").param("name", "Ana Perez"))
        .andExpect(status().isForbidden());
  }
}
