package org.iecr.diocesedashboard.webapp;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.LinkedHashSet;
import java.util.Set;

/** Factory that builds a {@link SecurityContext} containing a {@link DashboardUserDetails}. */
public class MockDashboardUserSecurityContextFactory
    implements WithSecurityContextFactory<WithMockDashboardUser> {

  @Override
  public SecurityContext createSecurityContext(WithMockDashboardUser annotation) {
    DashboardUser user = new DashboardUser();
    user.setId(1L);
    user.setUsername("testuser");
    user.setPasswordHash("$2a$10$mockhash");
    user.setRole(annotation.role());
    user.setPreferredLanguage(annotation.preferredLanguage());
    user.setEnabled(true);
    user.setAssignedChurches(buildChurches(annotation));
    DashboardUserDetails details = new DashboardUserDetails(user);
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
        details, "password", details.getAuthorities());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    return context;
  }

  private Set<Church> buildChurches(WithMockDashboardUser annotation) {
    Set<String> churchNames = new LinkedHashSet<>();
    addChurchName(churchNames, annotation.churchName());
    for (String churchName : annotation.churchNames()) {
      addChurchName(churchNames, churchName);
    }
    Set<Church> churches = new LinkedHashSet<>();
    for (String churchName : churchNames) {
      Church church = new Church();
      church.setName(churchName);
      churches.add(church);
    }
    return churches;
  }

  private void addChurchName(Set<String> churchNames, String churchName) {
    if (churchName != null && !churchName.isBlank()) {
      churchNames.add(churchName);
    }
  }
}
