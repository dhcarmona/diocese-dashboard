package org.iecr.diocesedashboard.webapp;

import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.service.UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.logging.Logger;

/** Creates the first ADMIN account on startup when explicitly configured. */
@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

  private static final Logger LOG =
      Logger.getLogger(BootstrapAdminInitializer.class.getName());

  private final BootstrapAdminProperties properties;
  private final UserService userService;

  /**
   * Creates the bootstrap initializer.
   *
   * @param properties configured bootstrap-admin properties
   * @param userService user management service
   */
  public BootstrapAdminInitializer(BootstrapAdminProperties properties, UserService userService) {
    this.properties = properties;
    this.userService = userService;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!properties.isEnabled()) {
      return;
    }
    validateConfiguration();
    if (userService.existsByRole(UserRole.ADMIN)) {
      LOG.info("Skipping bootstrap admin creation because an ADMIN user already exists.");
      return;
    }
    if (userService.existsByUsername(properties.getUsername())) {
      throw new IllegalStateException(
          "Bootstrap admin username already exists but no ADMIN user was found: "
              + properties.getUsername());
    }
    userService.createUser(
        properties.getUsername(), properties.getPassword(), UserRole.ADMIN, Set.of());
    LOG.info("Created the bootstrap ADMIN user: " + properties.getUsername());
  }

  private void validateConfiguration() {
    boolean hasUsername = StringUtils.hasText(properties.getUsername());
    boolean hasPassword = StringUtils.hasText(properties.getPassword());
    if (hasUsername != hasPassword) {
      throw new IllegalStateException(
          "Bootstrap admin configuration requires both username and password together.");
    }
    if (!hasUsername || !hasPassword) {
      throw new IllegalStateException(
          "Bootstrap admin is enabled, but username and password are not fully configured.");
    }
  }
}
