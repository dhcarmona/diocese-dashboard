package org.iecr.diocesedashboard.webapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ReporterLink;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.service.ReporterLinkService;
import org.iecr.diocesedashboard.service.ServiceSubmissionService;
import org.iecr.diocesedashboard.service.ServiceTemplateService;
import org.iecr.diocesedashboard.service.UserService;
import org.iecr.diocesedashboard.webapp.SecurityConfig;
import org.iecr.diocesedashboard.webapp.WithMockDashboardUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@WebMvcTest(ReporterLinkController.class)
@Import(SecurityConfig.class)
class ReporterLinkControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ReporterLinkService reporterLinkService;

  @MockBean
  private UserService userService;

  @MockBean
  private ServiceTemplateService serviceTemplateService;

  @MockBean
  private ServiceSubmissionService serviceSubmissionService;

  private static final String TOKEN = "test-token-uuid";

  @BeforeEach
  void setUp() {
    objectMapper.registerModule(new JavaTimeModule());
  }

  private DashboardUser buildReporter(Long id, String churchName) {
    DashboardUser user = new DashboardUser();
    user.setId(id);
    user.setUsername("reporter" + id);
    user.setRole(UserRole.REPORTER);
    Church church = new Church();
    church.setName(churchName);
    user.setChurch(church);
    return user;
  }

  private ServiceTemplate buildTemplate(Long id) {
    ServiceTemplate template = new ServiceTemplate();
    template.setId(id);
    template.setServiceTemplateName("Sunday Mass");
    return template;
  }

  private ReporterLink buildLink(String token, Long reporterId, String churchName) {
    ReporterLink link = new ReporterLink();
    link.setId(1L);
    link.setToken(token);
    link.setReporter(buildReporter(reporterId, churchName));
    link.setServiceTemplate(buildTemplate(2L));
    return link;
  }

  // --- GET /api/reporter-links ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAll_asAdmin_returns200() throws Exception {
    when(reporterLinkService.findAll()).thenReturn(List.of(buildLink(TOKEN, 5L, "Trinity")));

    mockMvc.perform(get("/api/reporter-links"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].token").value(TOKEN))
        .andExpect(jsonPath("$[0].reporterId").value(5L))
        .andExpect(jsonPath("$[0].serviceTemplateId").value(2L))
        .andExpect(jsonPath("$[0].reporter").doesNotExist())
        .andExpect(jsonPath("$[0].serviceTemplate").doesNotExist());
  }

  @Test
  @WithMockUser(roles = "REPORTER")
  void getAll_asReporter_returns403() throws Exception {
    mockMvc.perform(get("/api/reporter-links"))
        .andExpect(status().isForbidden());
  }

  @Test
  void getAll_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/reporter-links"))
        .andExpect(status().isUnauthorized());
  }

  // --- POST /api/reporter-links ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_asAdmin_validRequest_returns201() throws Exception {
    DashboardUser reporter = buildReporter(5L, "Trinity");
    ServiceTemplate template = buildTemplate(2L);
    ReporterLink link = buildLink(TOKEN, 5L, "Trinity");

    when(userService.findById(5L)).thenReturn(Optional.of(reporter));
    when(serviceTemplateService.findById(2L)).thenReturn(Optional.of(template));
    when(reporterLinkService.createLink(any(), any())).thenReturn(link);

    ReporterLinkRequest request = new ReporterLinkRequest(5L, 2L);
    mockMvc.perform(post("/api/reporter-links")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.token").value(TOKEN))
        .andExpect(jsonPath("$.reporterId").value(5L))
        .andExpect(jsonPath("$.serviceTemplateId").value(2L))
        .andExpect(jsonPath("$.reporter").doesNotExist())
        .andExpect(jsonPath("$.serviceTemplate").doesNotExist());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_reporterNotFound_returns404() throws Exception {
    when(userService.findById(99L)).thenReturn(Optional.empty());

    ReporterLinkRequest request = new ReporterLinkRequest(99L, 2L);
    mockMvc.perform(post("/api/reporter-links")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_targetUserIsAdmin_returns400() throws Exception {
    DashboardUser admin = new DashboardUser();
    admin.setId(3L);
    admin.setRole(UserRole.ADMIN);
    when(userService.findById(3L)).thenReturn(Optional.of(admin));

    ReporterLinkRequest request = new ReporterLinkRequest(3L, 2L);
    mockMvc.perform(post("/api/reporter-links")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_templateNotFound_returns404() throws Exception {
    when(userService.findById(5L)).thenReturn(Optional.of(buildReporter(5L, "Trinity")));
    when(serviceTemplateService.findById(99L)).thenReturn(Optional.empty());

    ReporterLinkRequest request = new ReporterLinkRequest(5L, 99L);
    mockMvc.perform(post("/api/reporter-links")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "REPORTER")
  void create_asReporter_returns403() throws Exception {
    ReporterLinkRequest request = new ReporterLinkRequest(5L, 2L);
    mockMvc.perform(post("/api/reporter-links")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  // --- GET /api/reporter-links/{token} ---

  @Test
  @WithMockDashboardUser
  void getByToken_asAdmin_exists_returns200() throws Exception {
    when(reporterLinkService.findByToken(TOKEN)).thenReturn(Optional.of(buildLink(TOKEN, 5L, "Trinity")));

    mockMvc.perform(get("/api/reporter-links/" + TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value(TOKEN))
        .andExpect(jsonPath("$.reporterId").value(5L))
        .andExpect(jsonPath("$.serviceTemplateId").value(2L))
        .andExpect(jsonPath("$.reporter").doesNotExist())
        .andExpect(jsonPath("$.serviceTemplate").doesNotExist());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER, churchName = "Trinity")
  void getByToken_asOwnerReporter_returns200() throws Exception {
    // MockDashboardUserSecurityContextFactory sets id=1L for the mock user
    when(reporterLinkService.findByToken(TOKEN)).thenReturn(Optional.of(buildLink(TOKEN, 1L, "Trinity")));

    mockMvc.perform(get("/api/reporter-links/" + TOKEN))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER, churchName = "Trinity")
  void getByToken_asNonOwnerReporter_returns404() throws Exception {
    // Link belongs to reporter id=99, but authenticated user is id=1
    when(reporterLinkService.findByToken(TOKEN)).thenReturn(Optional.of(buildLink(TOKEN, 99L, "Trinity")));

    mockMvc.perform(get("/api/reporter-links/" + TOKEN))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getByToken_notFound_returns404() throws Exception {
    when(reporterLinkService.findByToken("missing")).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/reporter-links/missing"))
        .andExpect(status().isNotFound());
  }

  @Test
  void getByToken_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/reporter-links/" + TOKEN))
        .andExpect(status().isUnauthorized());
  }

  // --- DELETE /api/reporter-links/{token} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_exists_returns204() throws Exception {
    when(reporterLinkService.existsByToken(TOKEN)).thenReturn(true);

    mockMvc.perform(delete("/api/reporter-links/" + TOKEN))
        .andExpect(status().isNoContent());

    verify(reporterLinkService).deleteByToken(TOKEN);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_notFound_returns404() throws Exception {
    when(reporterLinkService.existsByToken("missing")).thenReturn(false);

    mockMvc.perform(delete("/api/reporter-links/missing"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "REPORTER")
  void delete_asReporter_returns403() throws Exception {
    mockMvc.perform(delete("/api/reporter-links/" + TOKEN))
        .andExpect(status().isForbidden());
  }

  // --- POST /api/reporter-links/{token}/submit ---

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER, churchName = "Trinity")
  void submit_asOwnerReporter_returns201() throws Exception {
    // Mock reporter in link has id=1 (same as WithMockDashboardUser)
    when(reporterLinkService.findByToken(TOKEN)).thenReturn(
        Optional.of(buildLink(TOKEN, 1L, "Trinity")));
    ServiceInstance instance = new ServiceInstance();
    instance.setId(42L);
    when(serviceSubmissionService.submit(eq(2L), any(ServiceInstanceRequest.class)))
        .thenReturn(instance);

    ReporterLinkSubmitRequest request = new ReporterLinkSubmitRequest(
        List.of(10L), LocalDate.of(2024, 1, 14),
        List.of(new ServiceInstanceRequest.ResponseEntry(5L, "120")));

    mockMvc.perform(post("/api/reporter-links/" + TOKEN + "/submit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.serviceInstanceId").value(42));
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER, churchName = "Trinity")
  void submit_tokenNotFound_returns404() throws Exception {
    when(reporterLinkService.findByToken("missing")).thenReturn(Optional.empty());

    ReporterLinkSubmitRequest request = new ReporterLinkSubmitRequest(
        List.of(), LocalDate.of(2024, 1, 14), List.of());

    mockMvc.perform(post("/api/reporter-links/missing/submit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER, churchName = "Trinity")
  void submit_asNonOwnerReporter_returns403() throws Exception {
    // Link belongs to reporter id=99, authenticated user is id=1
    when(reporterLinkService.findByToken(TOKEN)).thenReturn(
        Optional.of(buildLink(TOKEN, 99L, "Trinity")));

    ReporterLinkSubmitRequest request = new ReporterLinkSubmitRequest(
        List.of(), LocalDate.of(2024, 1, 14), List.of());

    mockMvc.perform(post("/api/reporter-links/" + TOKEN + "/submit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void submit_asAdmin_returns403() throws Exception {
    ReporterLinkSubmitRequest request = new ReporterLinkSubmitRequest(
        List.of(), LocalDate.of(2024, 1, 14), List.of());

    mockMvc.perform(post("/api/reporter-links/" + TOKEN + "/submit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void submit_unauthenticated_returns401() throws Exception {
    ReporterLinkSubmitRequest request = new ReporterLinkSubmitRequest(
        List.of(), LocalDate.of(2024, 1, 14), List.of());

    mockMvc.perform(post("/api/reporter-links/" + TOKEN + "/submit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }
}
