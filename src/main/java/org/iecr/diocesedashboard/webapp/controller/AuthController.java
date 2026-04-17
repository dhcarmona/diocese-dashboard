package org.iecr.diocesedashboard.webapp.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.service.ReporterMagicLinkService;
import org.iecr.diocesedashboard.service.ReporterOtpService;
import org.iecr.diocesedashboard.service.UserService;
import org.iecr.diocesedashboard.webapp.DashboardUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Map;

/** REST controller for authenticated-session helper endpoints used by the SPA. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final UserService userService;
  private final ReporterOtpService reporterOtpService;
  private final ReporterMagicLinkService reporterMagicLinkService;
  private final String appBaseUrl;

  @Autowired
  public AuthController(UserService userService, ReporterOtpService reporterOtpService,
      ReporterMagicLinkService reporterMagicLinkService,
      @Value("${app.base-url}") String appBaseUrl) {
    this.userService = userService;
    this.reporterOtpService = reporterOtpService;
    this.reporterMagicLinkService = reporterMagicLinkService;
    this.appBaseUrl = appBaseUrl;
  }

  /**
   * Returns the current CSRF token so the SPA can send it on state-changing requests.
   *
   * @param csrfToken the current CSRF token
   * @return the token payload
   */
  @GetMapping("/csrf")
  public ResponseEntity<CsrfTokenResponse> getCsrfToken(CsrfToken csrfToken) {
    return ResponseEntity.ok(
        new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken()));
  }

  /**
   * Returns the authenticated user in a frontend-safe shape.
   *
   * @param authentication the current Spring Security authentication
   * @return the authenticated user payload
   */
  @GetMapping("/me")
  public ResponseEntity<AuthenticatedUserResponse> getAuthenticatedUser(
      Authentication authentication) {
    DashboardUser user =
        ((DashboardUserDetails) authentication.getPrincipal()).getDashboardUser();
    return ResponseEntity.ok(AuthenticatedUserResponse.from(user));
  }

  /**
   * Updates the authenticated user's preferred language.
   *
   * @param request        contains the preferred language code
   * @param authentication the current Spring Security authentication
   * @return the updated authenticated user payload
   */
  @PutMapping("/me/language")
  public ResponseEntity<AuthenticatedUserResponse> updatePreferredLanguage(
      @RequestBody @Valid PreferredLanguageRequest request,
      Authentication authentication) {
    DashboardUserDetails details = (DashboardUserDetails) authentication.getPrincipal();
    DashboardUser updated =
        userService.updatePreferredLanguage(details.getDashboardUser().getId(), request.language());
    details.getDashboardUser().setPreferredLanguage(updated.getPreferredLanguage());
    return ResponseEntity.ok(AuthenticatedUserResponse.from(updated));
  }

  /**
   * Generates and sends an OTP to the registered WhatsApp number of the given reporter user.
   * Always returns 200 for valid requests regardless of whether the username exists or whether
   * OTP dispatch succeeds, to prevent account enumeration. Returns 400 for invalid payloads
   * (e.g. blank username).
   *
   * @param request contains the reporter's username
   * @return 200 for valid requests; 400 for invalid payloads
   */
  @PostMapping("/reporter/request-otp")
  public ResponseEntity<Void> requestReporterOtp(
      @RequestBody @Valid ReporterOtpRequest request) {
    try {
      reporterOtpService.generateAndSendOtp(request.username());
    } catch (Exception ignored) {
      // Silently ignored to prevent account enumeration via differing response codes.
    }
    return ResponseEntity.ok().build();
  }

  /**
   * Verifies the reporter OTP and, if valid, creates an authenticated session.
   *
   * @param request     contains the reporter's username and OTP code
   * @param httpRequest the current HTTP request used to bind the new session
   * @return 200 on success, 401 if the code is invalid or expired, 429 if throttled
   */
  @PostMapping("/reporter/verify-otp")
  public ResponseEntity<?> verifyReporterOtp(
      @RequestBody @Valid ReporterOtpVerifyRequest request,
      HttpServletRequest httpRequest) {
    ReporterOtpService.OtpVerificationResult verificationResult =
        reporterOtpService.verifyAndConsumeOtp(request.username(), request.code());
    if (verificationResult.isThrottled()) {
      return buildTooManyRequestsResponse(
          "Please wait before trying another login code.",
          verificationResult.retryAfterSeconds());
    }
    if (verificationResult.isAttemptLimitExceeded()) {
      return buildTooManyRequestsResponse(
          "Too many login code attempts. Request a new code and try again later.",
          verificationResult.retryAfterSeconds());
    }
    if (!verificationResult.isSuccess()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }

    DashboardUser user = userService.findByUsername(request.username())
        .filter(uu -> uu.getRole() == UserRole.REPORTER && uu.isEnabled())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

    DashboardUserDetails userDetails = new DashboardUserDetails(user);
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
        userDetails, null, userDetails.getAuthorities());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);

    // Rotate the session ID to prevent session fixation attacks.
    HttpSession session = httpRequest.getSession(false);
    if (session != null) {
      httpRequest.changeSessionId();
      session = httpRequest.getSession(false);
    } else {
      session = httpRequest.getSession(true);
    }
    session.setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

    return ResponseEntity.ok().build();
  }

  /**
   * Sends a magic login link via WhatsApp to the reporter associated with the given username.
   * Always returns 200 for valid requests to prevent account enumeration.
   *
   * @param request     contains the reporter's username
   * @return 200 for valid requests; 400 for invalid payloads
   */
  @PostMapping("/reporter/request-login-link")
  public ResponseEntity<Void> requestReporterLoginLink(
      @RequestBody @Valid ReporterOtpRequest request) {
    try {
      String baseUrl = appBaseUrl;
      Locale localeHint = request.locale() != null && !request.locale().isBlank()
          ? Locale.forLanguageTag(request.locale())
          : null;
      reporterMagicLinkService.generateAndSendLoginLink(request.username(), baseUrl, localeHint);
    } catch (Exception ignored) {
      // Silently ignored to prevent account enumeration via differing response codes.
    }
    return ResponseEntity.ok().build();
  }

  /**
   * Redeems a magic login token and, if valid, creates an authenticated session.
   *
   * @param request     contains the login token string
   * @param httpRequest the current HTTP request used to bind the new session
   * @return 200 on success, 401 if the token is invalid or expired
   */
  @PostMapping("/reporter/redeem-login-token")
  public ResponseEntity<Void> redeemReporterLoginToken(
      @RequestBody @Valid ReporterLoginTokenRequest request,
      HttpServletRequest httpRequest) {
    DashboardUser user = reporterMagicLinkService.redeemToken(request.token())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

    DashboardUserDetails userDetails = new DashboardUserDetails(user);
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
        userDetails, null, userDetails.getAuthorities());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);

    // Rotate the session ID to prevent session fixation attacks.
    HttpSession session = httpRequest.getSession(false);
    if (session != null) {
      httpRequest.changeSessionId();
      session = httpRequest.getSession(false);
    } else {
      session = httpRequest.getSession(true);
    }
    session.setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

    return ResponseEntity.ok().build();
  }

  private ResponseEntity<Map<String, Object>> buildTooManyRequestsResponse(
      String message, long retryAfterSeconds) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds))
        .body(Map.of("status", HttpStatus.TOO_MANY_REQUESTS.value(), "message", message));
  }
}
