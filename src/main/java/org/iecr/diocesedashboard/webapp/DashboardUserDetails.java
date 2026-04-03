package org.iecr.diocesedashboard.webapp;

import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Spring Security {@link UserDetails} wrapper around a {@link DashboardUser}. */
public class DashboardUserDetails implements UserDetails {

  private final DashboardUser dashboardUser;

  /**
   * Constructs a new {@link DashboardUserDetails} wrapping the given user.
   *
   * @param dashboardUser the dashboard user entity to wrap
   */
  public DashboardUserDetails(DashboardUser dashboardUser) {
    this.dashboardUser = dashboardUser;
  }

  /**
   * Returns the underlying {@link DashboardUser} entity.
   *
   * @return the wrapped dashboard user
   */
  public DashboardUser getDashboardUser() {
    return dashboardUser;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + dashboardUser.getRole().name()));
  }

  @Override
  public String getPassword() {
    String hash = dashboardUser.getPasswordHash();
    return hash != null ? hash : "";
  }

  @Override
  public String getUsername() {
    return dashboardUser.getUsername();
  }

  @Override
  public boolean isEnabled() {
    return dashboardUser.isEnabled();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }
}
