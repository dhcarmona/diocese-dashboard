package org.iecr.diocesedashboard.webapp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configuration for optionally creating the first ADMIN user on startup. */
@Component
@ConfigurationProperties(prefix = "dashboard.bootstrap-admin")
public class BootstrapAdminProperties {

  private boolean enabled;
  private String username;
  private String password;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
