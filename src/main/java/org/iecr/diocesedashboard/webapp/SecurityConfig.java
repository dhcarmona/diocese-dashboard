package org.iecr.diocesedashboard.webapp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/** Spring Security configuration: HTTP Basic Auth with role-based access control. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
        // Auth and is currently intended for non-browser API clients that send credentials
        // explicitly in the Authorization header (never cookies). If a browser-based frontend
        // on the same origin starts calling these endpoints, or if cookie/session-based auth is
        // added in the future, CSRF protection must be re-enabled (e.g. CookieCsrfTokenRepository).
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/r/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/churches").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.GET, "/api/celebrants").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.GET, "/api/service-templates").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.GET, "/api/service-templates/*").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.POST, "/api/service-templates/*/submit").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.GET, "/api/service-instances").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.GET, "/api/service-instances/*").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.GET, "/api/reporter-links/*").hasAnyRole("ADMIN", "REPORTER")
                .requestMatchers(HttpMethod.POST, "/api/reporter-links/*/submit").hasAnyRole("ADMIN", "REPORTER")
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

}
