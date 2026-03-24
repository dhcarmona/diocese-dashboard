package org.iecr.diocesedashboard.webapp;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.Set;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminInitializerTest {

  @Mock
  private UserService userService;

  @Test
  void run_disabled_doesNothing() throws Exception {
    BootstrapAdminInitializer initializer =
        new BootstrapAdminInitializer(properties(false, "", ""), userService);

    initializer.run(new DefaultApplicationArguments(new String[0]));

    verify(userService, never()).createUser(ArgumentMatchers.any(),
        ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.anySet());
  }

  @Test
  void run_disabledWithPartialConfiguration_doesNothing() throws Exception {
    BootstrapAdminInitializer initializer =
        new BootstrapAdminInitializer(properties(false, "admin", ""), userService);

    initializer.run(new DefaultApplicationArguments(new String[0]));

    verify(userService, never()).createUser(ArgumentMatchers.any(),
        ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.anySet());
  }

  @Test
  void run_enabledWithoutPassword_throwsClearError() {
    BootstrapAdminInitializer initializer =
        new BootstrapAdminInitializer(properties(true, "admin", ""), userService);

    assertThatThrownBy(() -> initializer.run(new DefaultApplicationArguments(new String[0])))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("username and password");
  }

  @Test
  void run_enabledAndAdminExists_skipsCreation() throws Exception {
    when(userService.existsByRole(UserRole.ADMIN)).thenReturn(true);
    BootstrapAdminInitializer initializer =
        new BootstrapAdminInitializer(properties(true, "admin", "secret"), userService);

    initializer.run(new DefaultApplicationArguments(new String[0]));

    verify(userService).existsByRole(UserRole.ADMIN);
    verify(userService, never()).createUser(ArgumentMatchers.any(),
        ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.anySet());
  }

  @Test
  void run_enabledAndNoAdmin_createsBootstrapAdmin() throws Exception {
    when(userService.existsByRole(UserRole.ADMIN)).thenReturn(false);
    when(userService.existsByUsername("admin")).thenReturn(false);
    BootstrapAdminInitializer initializer =
        new BootstrapAdminInitializer(properties(true, "admin", "secret"), userService);

    initializer.run(new DefaultApplicationArguments(new String[0]));

    verify(userService).createUser("admin", "secret", UserRole.ADMIN, Set.of());
  }

  private BootstrapAdminProperties properties(boolean enabled, String username, String password) {
    BootstrapAdminProperties properties = new BootstrapAdminProperties();
    properties.setEnabled(enabled);
    properties.setUsername(username);
    properties.setPassword(password);
    return properties;
  }
}
