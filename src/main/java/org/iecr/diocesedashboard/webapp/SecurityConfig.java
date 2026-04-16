package org.iecr.diocesedashboard.webapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;

/**
 * Spring Security configuration: form-based login with session cookies
 * and CSRF protection via {@link HttpSessionCsrfTokenRepository}.
 */
@Configuration
@EnableWebSecurity
@Import(PasswordConfig.class)
public class SecurityConfig {

  private final UserDetailsService userDetailsService;
  private final PasswordEncoder passwordEncoder;
  private final ObjectMapper objectMapper;

  /**
   * Constructs SecurityConfig with the application's {@link UserDetailsService}.
   *
   * @param userDetailsService the service used to load users during authentication
   * @param passwordEncoder the password encoder used for authentication
   * @param objectMapper serializes structured JSON error responses
   */
  public SecurityConfig(
      UserDetailsService userDetailsService,
      PasswordEncoder passwordEncoder,
      ObjectMapper objectMapper) {
    this.userDetailsService = userDetailsService;
    this.passwordEncoder = passwordEncoder;
    this.objectMapper = objectMapper;
  }

  /**
   * Configures the security filter chain with session-based form login,
   * session-backed CSRF protection for the React SPA, and role-based authorization.
   *
   * @param http the {@link HttpSecurity} to configure
   * @return the configured {@link SecurityFilterChain}
   * @throws Exception if configuration fails
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    HttpSessionCsrfTokenRepository csrfTokenRepository = new HttpSessionCsrfTokenRepository();
    csrfTokenRepository.setHeaderName("X-CSRF-TOKEN");
    http
        .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                // Login and reporter OTP endpoints are exempt: the SPA fetches its CSRF token
                // from /api/auth/csrf after the initial page load and uses it for protected writes.
                // The public reporter-link submit endpoint is also exempt because it requires no
                // session cookie, so there is no CSRF attack surface.
                .ignoringRequestMatchers(
                    "/api/auth/login",
                    "/api/auth/reporter/request-otp",
                    "/api/auth/reporter/verify-otp",
                    "/api/auth/reporter/request-login-link",
                    "/api/auth/reporter/redeem-login-token",
                    "/api/reporter-links/public/*/submit")
        )
        .authorizeHttpRequests(auth -> auth
                .requestMatchers("/portraits/**").denyAll()
                .requestMatchers("/", "/index.html", "/assets/**", "/*.js",
                    "/*.css", "/*.ico", "/*.svg", "/*.png").permitAll()
                .requestMatchers(HttpMethod.GET, "/r/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/login", "/reports/new").permitAll()
                .requestMatchers(HttpMethod.GET, "/submit/service-templates/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/service-templates/manage").permitAll()
                .requestMatchers(HttpMethod.GET, "/users/manage").permitAll()
                .requestMatchers(HttpMethod.GET, "/celebrants/manage").permitAll()
                .requestMatchers(HttpMethod.GET, "/churches/manage").permitAll()
                .requestMatchers(HttpMethod.GET, "/reporter-links/manage").permitAll()
                .requestMatchers(HttpMethod.GET, "/whatsapp-logs").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/reports/view",
                    "/reports/view/individual",
                    "/reports/view/individual/*",
                    "/reports/view/individual/*/*").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/statistics",
                    "/statistics/*",
                    "/statistics/*/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reporter-links/public/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reporter-links/public/follow-up/*")
                .permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/api/reporter-links/public/*/submit").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/statistics").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/api/auth/reporter/request-otp",
                    "/api/auth/reporter/verify-otp",
                    "/api/auth/reporter/request-login-link",
                    "/api/auth/reporter/redeem-login-token").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/me").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.PUT, "/api/auth/me/language")
                .hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.GET, "/api/portraits/**").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.GET, "/api/churches").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.GET, "/api/celebrants").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.GET, "/api/service-templates").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.GET, "/api/service-templates/*").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.POST, "/api/service-templates/*/submit")
                .hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.GET, "/api/service-instances").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.GET, "/api/service-instances/*").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.PUT, "/api/service-instances/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/service-instances/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/reporter-links/*").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.POST, "/api/reporter-links/*/submit").hasRole("REPORTER")
                .anyRequest().hasRole("ADMIN")
        )
        .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .successHandler((req, res, auth) -> res.setStatus(HttpStatus.OK.value()))
                .failureHandler((req, res, ex) ->
                    writeAuthenticationFailureResponse(res, ex))
                .permitAll()
        )
        .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpStatus.OK.value()))
        )
        .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
        );
    return http.build();
  }

  @Bean
  Clock adminLoginClock() {
    return Clock.systemUTC();
  }

  @Bean
  AdminLoginThrottleService adminLoginThrottleService(Clock adminLoginClock) {
    return new AdminLoginThrottleService(adminLoginClock);
  }

  /**
   * Creates the {@link DaoAuthenticationProvider} backed by the application's
   * {@link UserDetailsService} and {@link PasswordEncoder}.
   *
   * @return configured authentication provider
   */
  @Bean
  public DaoAuthenticationProvider authenticationProvider(
      AdminLoginThrottleService adminLoginThrottleService) {
    DaoAuthenticationProvider provider =
        new RateLimitedDaoAuthenticationProvider(adminLoginThrottleService);
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return provider;
  }

  /**
   * Exposes the {@link AuthenticationManager} for use in custom auth flows.
   *
   * @param config Spring's authentication configuration
   * @return the authentication manager
   * @throws Exception if retrieval fails
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  private void writeAuthenticationFailureResponse(
      HttpServletResponse response, AuthenticationException ex)
      throws IOException {
    if (ex instanceof AdminLoginRateLimitException rateLimitEx) {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(rateLimitEx.getRetryAfterSeconds()));
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      objectMapper.writeValue(
          response.getWriter(),
          Map.of(
              "status", HttpStatus.TOO_MANY_REQUESTS.value(),
              "message", rateLimitEx.getMessage()));
      return;
    }
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
  }

  private static final class RateLimitedDaoAuthenticationProvider extends DaoAuthenticationProvider {

    private final AdminLoginThrottleService adminLoginThrottleService;

    private RateLimitedDaoAuthenticationProvider(AdminLoginThrottleService adminLoginThrottleService) {
      this.adminLoginThrottleService = adminLoginThrottleService;
    }

    @Override
    public Authentication authenticate(Authentication authentication)
        throws AuthenticationException {
      String username = authentication.getName();
      AdminLoginThrottleService.LoginAttemptResult preCheck =
          adminLoginThrottleService.checkAttemptAllowed(username);
      if (!preCheck.isAllowed()) {
        throw buildRateLimitException(preCheck);
      }

      try {
        Authentication result = super.authenticate(authentication);
        adminLoginThrottleService.clearFailedAttempts(username);
        return result;
      } catch (AuthenticationException ex) {
        AdminLoginThrottleService.LoginAttemptResult failureResult =
            adminLoginThrottleService.recordFailedAttempt(username);
        if (failureResult.isAttemptLimitExceeded()) {
          throw buildRateLimitException(failureResult);
        }
        throw ex;
      }
    }

    private AdminLoginRateLimitException buildRateLimitException(
        AdminLoginThrottleService.LoginAttemptResult result) {
      if (result.isAttemptLimitExceeded()) {
        return new AdminLoginRateLimitException(
            "Too many login attempts. Try again later.", result.retryAfterSeconds());
      }
      return new AdminLoginRateLimitException(
          "Please wait before trying to sign in again.", result.retryAfterSeconds());
    }
  }

  private static final class AdminLoginRateLimitException extends AuthenticationServiceException {

    private final long retryAfterSeconds;

    private AdminLoginRateLimitException(String message, long retryAfterSeconds) {
      super(message);
      this.retryAfterSeconds = retryAfterSeconds;
    }

    private long getRetryAfterSeconds() {
      return retryAfterSeconds;
    }
  }

}
