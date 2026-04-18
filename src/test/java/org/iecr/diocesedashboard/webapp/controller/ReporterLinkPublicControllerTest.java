package org.iecr.diocesedashboard.webapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.assertj.core.api.Assertions;
import org.iecr.diocesedashboard.domain.objects.Celebrant;
import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ReporterLink;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.service.CelebrantService;
import org.iecr.diocesedashboard.service.PortraitService;
import org.iecr.diocesedashboard.service.ReporterLinkFollowUpTokenService;
import org.iecr.diocesedashboard.service.ReporterLinkPublicSubmissionService;
import org.iecr.diocesedashboard.service.ReporterLinkService;
import org.iecr.diocesedashboard.service.UserService;
import org.iecr.diocesedashboard.webapp.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@WebMvcTest(ReporterLinkPublicController.class)
@Import(SecurityConfig.class)
class ReporterLinkPublicControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ReporterLinkService reporterLinkService;

  @MockBean
  private CelebrantService celebrantService;

  @MockBean
  private ReporterLinkPublicSubmissionService submissionService;

  @MockBean
  private ReporterLinkFollowUpTokenService followUpTokenService;

  @MockBean
  private UserService userService;

  @MockBean
  private PortraitService portraitService;

  private static final String TOKEN = "public-test-token-uuid";

  @BeforeEach
  void setUp() {
    objectMapper.registerModule(new JavaTimeModule());
  }

  private Church buildChurch(String churchName) {
    Church church = new Church();
    church.setName(churchName);
    return church;
  }

  private DashboardUser buildReporter(Long id, String... churchNames) {
    DashboardUser user = new DashboardUser();
    user.setId(id);
    user.setUsername("reporter" + id);
    user.setRole(UserRole.REPORTER);
    user.setAssignedChurches(Arrays.stream(churchNames)
        .map(this::buildChurch)
        .collect(Collectors.toSet()));
    return user;
  }

  private ServiceTemplate buildTemplate(Long id) {
    ServiceTemplate template = new ServiceTemplate();
    template.setId(id);
    template.setServiceTemplateName("Sunday Mass");
    template.setServiceInfoItems(Collections.emptyList());
    return template;
  }

  private ReporterLink buildLink(String token, Long reporterId, String churchName) {
    ReporterLink link = new ReporterLink();
    link.setId(1L);
    link.setToken(token);
    link.setReporter(buildReporter(reporterId, churchName));
    link.setChurch(buildChurch(churchName));
    link.setServiceTemplate(buildTemplate(2L));
    link.setActiveDate(LocalDate.now().minusDays(1));
    return link;
  }

  private Celebrant buildCelebrant(Long id, String name) {
    Celebrant celebrant = new Celebrant();
    celebrant.setId(id);
    celebrant.setName(name);
    return celebrant;
  }

  @Test
  void getByToken_validToken_returns200WithLinkData() throws Exception {
    when(reporterLinkService.findByToken(TOKEN)).thenReturn(
        Optional.of(buildLink(TOKEN, 5L, "Trinity")));
    when(celebrantService.findAll()).thenReturn(
        List.of(buildCelebrant(1L, "Fr. Smith"), buildCelebrant(2L, "Fr. Jones")));
    when(portraitService.buildServiceTemplateBannerUrl("Sunday Mass"))
        .thenReturn("/api/portraits/service-templates?name=Sunday+Mass");

    mockMvc.perform(get("/api/reporter-links/public/" + TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value(TOKEN))
        .andExpect(jsonPath("$.churchName").value("Trinity"))
        .andExpect(jsonPath("$.serviceTemplateName").value("Sunday Mass"))
        .andExpect(jsonPath("$.bannerUrl").value("/api/portraits/service-templates?name=Sunday+Mass"))
        .andExpect(jsonPath("$.celebrants.length()").value(2))
        .andExpect(jsonPath("$.celebrants[0].name").value("Fr. Smith"));
  }

  @Test
  void getByToken_unknownToken_returns404() throws Exception {
    when(reporterLinkService.findByToken("bad-token")).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/reporter-links/public/bad-token"))
        .andExpect(status().isNotFound());
  }

  @Test
  void getByToken_noCredentialsRequired_publiclyAccessible() throws Exception {
    when(reporterLinkService.findByToken(TOKEN)).thenReturn(
        Optional.of(buildLink(TOKEN, 5L, "Trinity")));
    when(celebrantService.findAll()).thenReturn(List.of());

    // No authentication headers — should still succeed
    mockMvc.perform(get("/api/reporter-links/public/" + TOKEN))
        .andExpect(status().isOk());
  }

  @Test
  void submit_activeLink_returns201AndClaimsToken() throws Exception {
    ReporterLink link = buildLink(TOKEN, 5L, "Trinity");
    link.setActiveDate(LocalDate.of(2024, 1, 20));
    when(reporterLinkService.findByToken(TOKEN)).thenReturn(Optional.of(link));
    ServiceInstance instance = new ServiceInstance();
    instance.setId(42L);
    when(submissionService.claimAndSubmit(any(ReporterLink.class),
        any(ServiceInstanceRequest.class), any(DashboardUser.class)))
        .thenReturn(Optional.of(instance));
    ReporterLink nextLink = buildLink("next-public-token", 5L, "Trinity");
    nextLink.setActiveDate(LocalDate.of(2026, 4, 15));
    when(reporterLinkService.findNextPendingLinkForReporter(any(DashboardUser.class)))
        .thenReturn(Optional.of(nextLink));
    when(followUpTokenService.createToken(any(DashboardUser.class))).thenReturn("follow-up-token");

    ReporterLinkSubmitRequest request = new ReporterLinkSubmitRequest(
        List.of(10L),
        List.of(new ServiceInstanceRequest.ResponseEntry(5L, "120")));

    mockMvc.perform(post("/api/reporter-links/public/" + TOKEN + "/submit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.serviceInstanceId").value(42))
        .andExpect(jsonPath("$.nextReporterLinkToken").doesNotExist())
        .andExpect(jsonPath("$.nextReporterLinkFollowUpToken").value("follow-up-token"))
        .andExpect(jsonPath("$.nextReporterLinkActiveDate").value("2026-04-15"));

    ArgumentCaptor<ServiceInstanceRequest> captor =
        ArgumentCaptor.forClass(ServiceInstanceRequest.class);
    verify(submissionService).claimAndSubmit(any(ReporterLink.class), captor.capture(),
        any(DashboardUser.class));
    Assertions.assertThat(captor.getValue().serviceDate()).isEqualTo(LocalDate.of(2024, 1, 20));
  }

  @Test
  void submit_unknownToken_returns404() throws Exception {
    when(reporterLinkService.findByToken("bad-token")).thenReturn(Optional.empty());

    ReporterLinkSubmitRequest request = new ReporterLinkSubmitRequest(
        List.of(), List.of());

    mockMvc.perform(post("/api/reporter-links/public/bad-token/submit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  void getNextPendingLink_validFollowUpToken_returns200() throws Exception {
    DashboardUser reporter = buildReporter(5L, "Trinity");
    ReporterLink nextLink = buildLink("next-public-token", 5L, "Trinity");
    nextLink.setActiveDate(LocalDate.of(2026, 4, 15));
    when(followUpTokenService.resolveReporterId("follow-up-token")).thenReturn(Optional.of(5L));
    when(userService.findById(5L)).thenReturn(Optional.of(reporter));
    when(reporterLinkService.findNextPendingLinkForReporter(reporter))
        .thenReturn(Optional.of(nextLink));

    mockMvc.perform(get("/api/reporter-links/public/follow-up/follow-up-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nextReporterLinkToken").value("next-public-token"))
        .andExpect(jsonPath("$.nextReporterLinkActiveDate").value("2026-04-15"));
  }

  @Test
  void submit_notYetActiveLink_returns409() throws Exception {
    ReporterLink futureLink = buildLink(TOKEN, 5L, "Trinity");
    futureLink.setActiveDate(LocalDate.now().plusDays(1));
    when(reporterLinkService.findByToken(TOKEN)).thenReturn(Optional.of(futureLink));

    ReporterLinkSubmitRequest request = new ReporterLinkSubmitRequest(
        List.of(), List.of());

    mockMvc.perform(post("/api/reporter-links/public/" + TOKEN + "/submit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  @Test
  void submit_noCsrfRequired_publiclyAccessible() throws Exception {
    when(reporterLinkService.findByToken(TOKEN)).thenReturn(
        Optional.of(buildLink(TOKEN, 5L, "Trinity")));
    ServiceInstance instance = new ServiceInstance();
    instance.setId(7L);
    when(submissionService.claimAndSubmit(any(ReporterLink.class),
        any(ServiceInstanceRequest.class), any(DashboardUser.class)))
        .thenReturn(Optional.of(instance));

    ReporterLinkSubmitRequest request = new ReporterLinkSubmitRequest(
        List.of(), List.of());

    // No CSRF header, no authentication — should still succeed
    mockMvc.perform(post("/api/reporter-links/public/" + TOKEN + "/submit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  @Test
  void submit_tokenAlreadyClaimed_returns409() throws Exception {
    when(reporterLinkService.findByToken(TOKEN)).thenReturn(
        Optional.of(buildLink(TOKEN, 5L, "Trinity")));
    when(submissionService.claimAndSubmit(any(ReporterLink.class),
        any(ServiceInstanceRequest.class), any(DashboardUser.class)))
        .thenReturn(Optional.empty());

    ReporterLinkSubmitRequest request = new ReporterLinkSubmitRequest(
        List.of(), List.of());

    mockMvc.perform(post("/api/reporter-links/public/" + TOKEN + "/submit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  @Test
  void submit_reporterNotAssignedToChurch_returns403() throws Exception {
    ReporterLink link = buildLink(TOKEN, 5L, "Trinity");
    DashboardUser wrongReporter = buildReporter(5L, "OtherChurch");
    link.setReporter(wrongReporter);
    when(reporterLinkService.findByToken(TOKEN)).thenReturn(Optional.of(link));

    ReporterLinkSubmitRequest request = new ReporterLinkSubmitRequest(
        List.of(), List.of());

    mockMvc.perform(post("/api/reporter-links/public/" + TOKEN + "/submit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void submit_reporterAccountDisabled_returns403() throws Exception {
    ReporterLink link = buildLink(TOKEN, 5L, "Trinity");
    DashboardUser disabledReporter = buildReporter(5L, "Trinity");
    disabledReporter.setEnabled(false);
    link.setReporter(disabledReporter);
    when(reporterLinkService.findByToken(TOKEN)).thenReturn(Optional.of(link));

    ReporterLinkSubmitRequest request = new ReporterLinkSubmitRequest(
        List.of(), List.of());

    mockMvc.perform(post("/api/reporter-links/public/" + TOKEN + "/submit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }
}
