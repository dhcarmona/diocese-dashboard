package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ReporterLoginToken;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages WhatsApp magic-link login for reporter users.
 *
 * <p>Tokens are random UUIDs stored in the database with a fixed TTL. On request,
 * any prior token for the user is invalidated and a new one is sent via WhatsApp.
 * A minimum interval between requests is enforced to prevent message flooding.
 *
 * <p>To avoid leaking account existence, this service is intentionally silent when a
 * username is not found or not eligible.
 */
@Service
public class ReporterMagicLinkService {

  /** How long a login token remains valid. */
  static final Duration TOKEN_TTL = Duration.ofMinutes(10);

  /** Minimum time between consecutive link requests for the same user. */
  static final Duration REQUEST_INTERVAL = Duration.ofSeconds(60);

  private final ReporterLoginTokenService tokenService;
  private final UserService userService;
  private final WhatsAppService whatsAppService;
  private final MessageSource messageSource;
  private final Clock clock;

  @Autowired
  public ReporterMagicLinkService(
      ReporterLoginTokenService tokenService,
      UserService userService,
      WhatsAppService whatsAppService,
      MessageSource messageSource) {
    this(tokenService, userService, whatsAppService, messageSource, Clock.systemUTC());
  }

  ReporterMagicLinkService(
      ReporterLoginTokenService tokenService,
      UserService userService,
      WhatsAppService whatsAppService,
      MessageSource messageSource,
      Clock clock) {
    this.tokenService = tokenService;
    this.userService = userService;
    this.whatsAppService = whatsAppService;
    this.messageSource = messageSource;
    this.clock = clock;
  }

  /**
   * Generates and sends a magic login link to the reporter's registered WhatsApp number.
   * Silently does nothing if the username is unknown, not a reporter, disabled, or
   * has no phone number. Enforces a minimum request interval per user.
   * Any prior unexpired token for this user is invalidated before issuing a new one.
   *
   * @param username     the reporter's username
   * @param loginBaseUrl the base URL of the application, used to build the login link
   * @param localeHint   optional locale provided by the client; overrides the stored preference
   */
  @Transactional
  public void generateAndSendLoginLink(String username, String loginBaseUrl, Locale localeHint) {
    if (username == null || username.isBlank()) {
      return;
    }

    Instant now = Instant.now(clock);

    // Silently enforce request interval — do not reveal whether the user exists.
    if (tokenService.existsByUsernameAndCreatedAtAfter(username, now.minus(REQUEST_INTERVAL))) {
      return;
    }

    Optional<DashboardUser> userOpt = userService.findByUsername(username)
        .filter(DashboardUser::isEnabled)
        .filter(uu -> uu.getRole() == UserRole.REPORTER)
        .filter(uu -> uu.getPhoneNumber() != null && !uu.getPhoneNumber().isBlank());

    if (userOpt.isEmpty()) {
      return;
    }

    DashboardUser user = userOpt.get();

    Locale effectiveLocale = localeHint != null ? localeHint : user.getPreferredLocale();

    // Invalidate any prior tokens before issuing a fresh one.
    tokenService.deleteByUsername(username);

    String tokenValue = UUID.randomUUID().toString();
    String loginUrl = buildLoginUrl(loginBaseUrl, tokenValue);

    ReporterLoginToken loginToken = new ReporterLoginToken();
    loginToken.setToken(tokenValue);
    loginToken.setUsername(username);
    loginToken.setCreatedAt(now);
    loginToken.setExpiresAt(now.plus(TOKEN_TTL));
    tokenService.save(loginToken);

    String body = messageSource.getMessage(
        "magic.link.whatsapp.message",
        new Object[]{loginUrl},
        "Access Diocese Dashboard with this link: " + loginUrl + " It expires in 10 minutes.",
        effectiveLocale);

    // Log a redacted summary — never store the actual token URL in the message log.
    String logSummary = messageSource.getMessage(
        "magic.link.whatsapp.message",
        new Object[]{"[login-link]"},
        "Access Diocese Dashboard with this link: [login-link] It expires in 10 minutes.",
        effectiveLocale);

    whatsAppService.sendConfiguredMessageAndLog(
        user.getPhoneNumber(),
        body,
        logSummary,
        username,
        effectiveLocale,
        WhatsAppService.TemplateType.REPORTER_LOGIN_LINK,
        Map.of("1", tokenValue));
  }

  /**
   * Redeems a magic login token and returns the corresponding reporter user if valid.
   * The token is consumed (deleted) on first successful use.
   * Expired tokens are rejected and cleaned up.
   *
   * @param tokenValue the token string from the magic link URL
   * @return the authenticated reporter user, or empty if the token is invalid or expired
   */
  @Transactional
  public Optional<DashboardUser> redeemToken(String tokenValue) {
    if (tokenValue == null || tokenValue.isBlank()) {
      return Optional.empty();
    }

    Optional<ReporterLoginToken> tokenOpt = tokenService.findByToken(tokenValue);
    if (tokenOpt.isEmpty()) {
      return Optional.empty();
    }

    ReporterLoginToken token = tokenOpt.get();

    if (!tokenService.claimToken(tokenValue)) {
      return Optional.empty();
    }

    if (Instant.now(clock).isAfter(token.getExpiresAt())) {
      return Optional.empty();
    }

    return userService.findByUsername(token.getUsername())
        .filter(DashboardUser::isEnabled)
        .filter(uu -> uu.getRole() == UserRole.REPORTER);
  }

  /**
   * Deletes expired tokens from the database. Intended for periodic cleanup.
   */
  @Transactional
  public void pruneExpiredTokens() {
    tokenService.deleteExpiredBefore(Instant.now(clock));
  }

  private String buildLoginUrl(String loginBaseUrl, String token) {
    String base = loginBaseUrl == null ? "" : loginBaseUrl;
    if (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    return base + "/login?token=" + token;
  }
}
