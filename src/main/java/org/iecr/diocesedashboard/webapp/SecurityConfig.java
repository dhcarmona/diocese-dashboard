package org.iecr.diocesedashboard.webapp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

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

  /**
   * Constructs SecurityConfig with the application's {@link UserDetailsService}.
   *
   * @param userDetailsService the service used to load users during authentication
   * @param passwordEncoder the password encoder used for authentication
   */
  public SecurityConfig(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    this.userDetailsService = userDetailsService;
    this.passwordEncoder = passwordEncoder;
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
                .ignoringRequestMatchers(
                    "/api/auth/login",
                    "/api/auth/reporter/request-otp",
                    "/api/auth/reporter/verify-otp")
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
                .requestMatchers(HttpMethod.GET,
                    "/reports/view",
                    "/reports/view/individual",
                    "/reports/view/individual/*",
                    "/reports/view/individual/*/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/api/auth/reporter/request-otp",
                    "/api/auth/reporter/verify-otp").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/me").hasAnyRole("ADMIN", "REPORTER")
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
                .failureHandler((req, res, ex) -> res.setStatus(HttpStatus.UNAUTHORIZED.value()))
                .permitAll()
        )
        .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpStatus.OK.value()))
        )
        .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
        )
        .authenticationProvider(authenticationProvider());
    return http.build();
  }

  /**
   * Creates the {@link DaoAuthenticationProvider} backed by the application's
   * {@link UserDetailsService} and {@link PasswordEncoder}.
   *
   * @return configured authentication provider
   */
  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
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

}
