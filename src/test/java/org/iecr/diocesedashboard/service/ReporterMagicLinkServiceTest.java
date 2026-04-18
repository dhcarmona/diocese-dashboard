package org.iecr.diocesedashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ReporterLoginToken;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ReporterMagicLinkServiceTest {

  @Mock
  private ReporterLoginTokenService tokenService;
  @Mock
  private UserService userService;
  @Mock
  private WhatsAppService whatsAppService;
  @Mock
  private MessageSource messageSource;

  private static final Instant FIXED_NOW = Instant.parse("2026-01-01T12:00:00Z");
  private final Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));

  private ReporterMagicLinkService service;

  private DashboardUser activeReporter(String username) {
    DashboardUser user = new DashboardUser();
    user.setUsername(username);
    user.setEnabled(true);
    user.setRole(UserRole.REPORTER);
    user.setPhoneNumber("+50688888888");
    user.setPreferredLanguage("es");
    return user;
  }

  @BeforeEach
  void setUp() {
    service = new ReporterMagicLinkService(
        tokenService, userService, whatsAppService, messageSource, fixedClock);
  }

  @Test
  void generateAndSendLoginLink_savesTokenAndSendsWhatsApp() {
    DashboardUser user = activeReporter("reporter1");
    when(tokenService.existsByUsernameAndCreatedAtAfter(eq("reporter1"), any())).thenReturn(false);
    when(userService.findByUsername("reporter1")).thenReturn(Optional.of(user));
    when(messageSource.getMessage(any(), any(), any(), any(Locale.class))).thenReturn("msg");

    service.generateAndSendLoginLink("reporter1", "http://localhost:5173", null);

    verify(tokenService).save(argThat(tt -> "reporter1".equals(tt.getUsername())
        && tt.getExpiresAt().equals(FIXED_NOW.plusSeconds(600))));
    verify(whatsAppService).sendConfiguredMessageAndLog(
        eq("+50688888888"), any(), any(), eq("reporter1"),
        eq(WhatsAppService.TemplateType.REPORTER_LOGIN_LINK), any());
  }

  @Test
  void generateAndSendLoginLink_respectsRequestInterval() {
    when(tokenService.existsByUsernameAndCreatedAtAfter(eq("reporter1"), any())).thenReturn(true);

    service.generateAndSendLoginLink("reporter1", "http://localhost:5173", null);

    verify(userService, never()).findByUsername(any());
    verify(whatsAppService, never()).sendConfiguredMessageAndLog(
        any(), any(), any(), any(), any(), any());
  }

  @Test
  void generateAndSendLoginLink_silentForUnknownUsername() {
    when(tokenService.existsByUsernameAndCreatedAtAfter(any(), any())).thenReturn(false);
    when(userService.findByUsername("unknown")).thenReturn(Optional.empty());

    service.generateAndSendLoginLink("unknown", "http://localhost:5173", null);

    verify(whatsAppService, never()).sendConfiguredMessageAndLog(
        any(), any(), any(), any(), any(), any());
  }

  @Test
  void generateAndSendLoginLink_silentForDisabledUser() {
    DashboardUser user = activeReporter("disabled1");
    user.setEnabled(false);
    when(tokenService.existsByUsernameAndCreatedAtAfter(any(), any())).thenReturn(false);
    when(userService.findByUsername("disabled1")).thenReturn(Optional.of(user));

    service.generateAndSendLoginLink("disabled1", "http://localhost:5173", null);

    verify(whatsAppService, never()).sendConfiguredMessageAndLog(
        any(), any(), any(), any(), any(), any());
  }

  @Test
  void generateAndSendLoginLink_silentForNonReporter() {
    DashboardUser user = activeReporter("admin1");
    user.setRole(UserRole.ADMIN);
    when(tokenService.existsByUsernameAndCreatedAtAfter(any(), any())).thenReturn(false);
    when(userService.findByUsername("admin1")).thenReturn(Optional.of(user));

    service.generateAndSendLoginLink("admin1", "http://localhost:5173", null);

    verify(whatsAppService, never()).sendConfiguredMessageAndLog(
        any(), any(), any(), any(), any(), any());
  }

  @Test
  void generateAndSendLoginLink_silentForUserWithoutPhone() {
    DashboardUser user = activeReporter("nophone");
    user.setPhoneNumber(null);
    when(tokenService.existsByUsernameAndCreatedAtAfter(any(), any())).thenReturn(false);
    when(userService.findByUsername("nophone")).thenReturn(Optional.of(user));

    service.generateAndSendLoginLink("nophone", "http://localhost:5173", null);

    verify(whatsAppService, never()).sendConfiguredMessageAndLog(
        any(), any(), any(), any(), any(), any());
  }

  @Test
  void generateAndSendLoginLink_withLocaleHint_sendsLoginLinkTemplate() {
    DashboardUser user = activeReporter("reporter1");
    when(tokenService.existsByUsernameAndCreatedAtAfter(any(), any())).thenReturn(false);
    when(userService.findByUsername("reporter1")).thenReturn(Optional.of(user));
    when(messageSource.getMessage(any(), any(), any(), any(Locale.class))).thenReturn("msg");

    service.generateAndSendLoginLink("reporter1", "http://localhost:5173", Locale.ENGLISH);

    verify(whatsAppService).sendConfiguredMessageAndLog(
        any(), any(), any(), any(),
        eq(WhatsAppService.TemplateType.REPORTER_LOGIN_LINK), any());
  }

  @Test
  void redeemToken_validTokenReturnsUserAndDeletesToken() {
    ReporterLoginToken token = new ReporterLoginToken();
    token.setToken("abc-token");
    token.setUsername("reporter1");
    token.setExpiresAt(FIXED_NOW.plusSeconds(300));

    DashboardUser user = activeReporter("reporter1");
    when(tokenService.findByToken("abc-token")).thenReturn(Optional.of(token));
    when(tokenService.claimToken("abc-token")).thenReturn(true);
    when(userService.findByUsername("reporter1")).thenReturn(Optional.of(user));

    Optional<DashboardUser> result = service.redeemToken("abc-token");

    assertTrue(result.isPresent());
    assertEquals("reporter1", result.get().getUsername());
  }

  @Test
  void redeemToken_expiredTokenReturnsEmpty() {
    ReporterLoginToken token = new ReporterLoginToken();
    token.setToken("old-token");
    token.setUsername("reporter1");
    token.setExpiresAt(FIXED_NOW.minusSeconds(1));

    when(tokenService.findByToken("old-token")).thenReturn(Optional.of(token));
    when(tokenService.claimToken("old-token")).thenReturn(true);

    Optional<DashboardUser> result = service.redeemToken("old-token");

    assertTrue(result.isEmpty());
  }

  @Test
  void redeemToken_unknownTokenReturnsEmpty() {
    when(tokenService.findByToken("bad-token")).thenReturn(Optional.empty());

    Optional<DashboardUser> result = service.redeemToken("bad-token");

    assertTrue(result.isEmpty());
  }

  @Test
  void redeemToken_alreadyConsumedTokenReturnsEmpty() {
    ReporterLoginToken token = new ReporterLoginToken();
    token.setToken("raced-token");
    token.setUsername("reporter1");
    token.setExpiresAt(FIXED_NOW.plusSeconds(300));

    when(tokenService.findByToken("raced-token")).thenReturn(Optional.of(token));
    when(tokenService.claimToken("raced-token")).thenReturn(false);

    Optional<DashboardUser> result = service.redeemToken("raced-token");

    assertTrue(result.isEmpty());
  }

  @Test
  void redeemToken_nonReporterUserReturnsEmpty() {
    ReporterLoginToken token = new ReporterLoginToken();
    token.setToken("admin-token");
    token.setUsername("admin1");
    token.setExpiresAt(FIXED_NOW.plusSeconds(300));

    DashboardUser user = activeReporter("admin1");
    user.setRole(UserRole.ADMIN);
    when(tokenService.findByToken("admin-token")).thenReturn(Optional.of(token));
    when(tokenService.claimToken("admin-token")).thenReturn(true);
    when(userService.findByUsername("admin1")).thenReturn(Optional.of(user));

    Optional<DashboardUser> result = service.redeemToken("admin-token");

    assertTrue(result.isEmpty());
  }
}
