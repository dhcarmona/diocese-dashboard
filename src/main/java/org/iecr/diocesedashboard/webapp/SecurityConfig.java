package org.iecr.diocesedashboard.webapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/** Spring Security configuration: HTTP Basic Auth with role-based access control. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Value("${DASHBOARD_ADMIN_USERNAME}")
  private String adminUsername;

  @Value("${DASHBOARD_ADMIN_PASSWORD}")
  private String adminPassword;

  @Value("${DASHBOARD_USER_USERNAME}")
  private String userUsername;

  @Value("${DASHBOARD_USER_PASSWORD}")
  private String userPassword;

  /**
   * Configures the security filter chain with CSRF disabled, role-based authorization rules,
   * and HTTP Basic authentication.
   *
   * @param http the {@link HttpSecurity} to configure
   * @return the configured {@link SecurityFilterChain}
   * @throws Exception if configuration fails
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // CSRF is disabled intentionally: this is a stateless REST API secured with HTTP Basic
        // Auth. Clients authenticate via the Authorization header (never cookies), so browsers
        // cannot forge cross-site requests. If cookie/session-based auth is added in the future,
        // CSRF protection must be re-enabled (e.g. CookieCsrfTokenRepository).
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/churches").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.GET, "/api/celebrants").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.GET, "/api/service-templates/*").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.POST, "/api/service-templates/*/submit").hasAnyRole("ADMIN", "USER")
                .anyRequest().hasRole("ADMIN")
        )
        .httpBasic(Customizer.withDefaults());
    return http.build();
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

  /**
   * In-memory user store backed by environment variables.
   *
   * <p>Required environment variables:
   * <ul>
   *   <li>DASHBOARD_ADMIN_USERNAME</li>
   *   <li>DASHBOARD_ADMIN_PASSWORD</li>
   *   <li>DASHBOARD_USER_USERNAME</li>
   *   <li>DASHBOARD_USER_PASSWORD</li>
   * </ul>
   */
  @Bean
  public UserDetailsService userDetailsService(PasswordEncoder encoder) {
    UserDetails admin = User.builder()
        .username(adminUsername)
        .password(encoder.encode(adminPassword))
        .roles("ADMIN")
        .build();
    UserDetails user = User.builder()
        .username(userUsername)
        .password(encoder.encode(userPassword))
        .roles("USER")
        .build();
    return new InMemoryUserDetailsManager(admin, user);
  }
}
