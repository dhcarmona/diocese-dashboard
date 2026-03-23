package org.iecr.diocesedashboard.webapp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Spring Security configuration: form-based login with session cookies
 * and CSRF protection via {@link CookieCsrfTokenRepository}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final UserDetailsService userDetailsService;

  /**
   * Constructs SecurityConfig with the application's {@link UserDetailsService}.
   *
   * @param userDetailsService the service used to load users during authentication
   */
  public SecurityConfig(UserDetailsService userDetailsService) {
    this.userDetailsService = userDetailsService;
  }

  /**
   * Configures the security filter chain with session-based form login,
   * CSRF cookie support for the React SPA, and role-based authorization.
   *
   * @param http the {@link HttpSecurity} to configure
   * @return the configured {@link SecurityFilterChain}
   * @throws Exception if configuration fails
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                // Login endpoint is exempt: the CSRF cookie is set on page load before the first login
                .ignoringRequestMatchers("/api/auth/login")
        )
        .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/assets/**", "/*.js",
                    "/*.css", "/*.ico", "/*.svg", "/*.png").permitAll()
                .requestMatchers(HttpMethod.GET, "/r/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/login").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/churches").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.GET, "/api/celebrants").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.GET, "/api/service-templates").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.GET, "/api/service-templates/*").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.POST, "/api/service-templates/*/submit")
                .hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.GET, "/api/service-instances").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.GET, "/api/service-instances/*").hasAnyRole("ADMIN", "REPORTER")
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
    provider.setPasswordEncoder(passwordEncoder());
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

  /**
   * Returns a BCrypt password encoder.
   *
   * @return a {@link BCryptPasswordEncoder} instance
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

}
