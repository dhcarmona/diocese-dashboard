package org.iecr.diocesedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.domain.repositories.UserRepository;
import org.iecr.diocesedashboard.webapp.DashboardUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private UserService userService;

  private DashboardUser buildUser(Long id, String username, UserRole role) {
    DashboardUser user = new DashboardUser();
    user.setId(id);
    user.setUsername(username);
    user.setPasswordHash("$2a$10$existinghash");
    user.setRole(role);
    user.setEnabled(true);
    return user;
  }

  // --- loadUserByUsername ---

  @Test
  void loadUserByUsername_found_returnsDashboardUserDetails() {
    DashboardUser user = buildUser(1L, "admin", UserRole.ADMIN);
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

    UserDetails result = userService.loadUserByUsername("admin");

    assertThat(result).isInstanceOf(DashboardUserDetails.class);
    assertThat(result.getUsername()).isEqualTo("admin");
    assertThat(result.getAuthorities()).extracting("authority")
        .containsExactly("ROLE_ADMIN");
  }

  @Test
  void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.loadUserByUsername("ghost"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("ghost");
  }

  // --- createUser ---

  @Test
  void createUser_encodesPassword_andSaves() {
    when(passwordEncoder.encode("plaintext")).thenReturn("$2a$10$encoded");
    when(userRepository.save(any(DashboardUser.class))).thenAnswer(inv -> inv.getArgument(0));

    DashboardUser result = userService.createUser("reporter1", "plaintext",
        UserRole.REPORTER, Set.of());

    verify(passwordEncoder).encode("plaintext");
    assertThat(result.getPasswordHash()).isEqualTo("$2a$10$encoded");
  }

  @Test
  void createUser_setsAllFields() {
    Church church = new Church();
    church.setName("Trinity");
    when(passwordEncoder.encode(any())).thenReturn("hash");
    when(userRepository.save(any(DashboardUser.class))).thenAnswer(inv -> inv.getArgument(0));

    DashboardUser result = userService.createUser("rep", "pass",
        UserRole.REPORTER, Set.of(church));

    assertThat(result.getUsername()).isEqualTo("rep");
    assertThat(result.getRole()).isEqualTo(UserRole.REPORTER);
    assertThat(result.getAssignedChurches()).containsExactly(church);
    assertThat(result.isEnabled()).isTrue();
  }

  // --- updateUser ---

  @Test
  void updateUser_withNewPassword_encodesPassword() {
    DashboardUser existing = buildUser(1L, "user1", UserRole.REPORTER);
    when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(passwordEncoder.encode("newpass")).thenReturn("$2a$10$newEncoded");
    when(userRepository.save(any(DashboardUser.class))).thenAnswer(inv -> inv.getArgument(0));

    DashboardUser result = userService.updateUser(1L, "user1", "newpass",
        UserRole.REPORTER, Set.of());

    verify(passwordEncoder).encode("newpass");
    assertThat(result.getPasswordHash()).isEqualTo("$2a$10$newEncoded");
  }

  @Test
  void updateUser_withBlankPassword_keepsExistingHash() {
    DashboardUser existing = buildUser(1L, "user1", UserRole.REPORTER);
    when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(userRepository.save(any(DashboardUser.class))).thenAnswer(inv -> inv.getArgument(0));

    DashboardUser result = userService.updateUser(1L, "user1", "",
        UserRole.REPORTER, Set.of());

    verify(passwordEncoder, never()).encode(any());
    assertThat(result.getPasswordHash()).isEqualTo("$2a$10$existinghash");
  }

  @Test
  void updateUser_withNullPassword_keepsExistingHash() {
    DashboardUser existing = buildUser(1L, "user1", UserRole.REPORTER);
    when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(userRepository.save(any(DashboardUser.class))).thenAnswer(inv -> inv.getArgument(0));

    DashboardUser result = userService.updateUser(1L, "user1", null,
        UserRole.REPORTER, Set.of());

    verify(passwordEncoder, never()).encode(any());
    assertThat(result.getPasswordHash()).isEqualTo("$2a$10$existinghash");
  }

  @Test
  void updateUser_replacesAssignedChurches() {
    Church oldChurch = new Church();
    oldChurch.setName("Trinity");
    Church newChurch = new Church();
    newChurch.setName("StPaul");
    DashboardUser existing = buildUser(1L, "user1", UserRole.REPORTER);
    existing.setAssignedChurches(Set.of(oldChurch));
    when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(userRepository.save(any(DashboardUser.class))).thenAnswer(inv -> inv.getArgument(0));

    DashboardUser result = userService.updateUser(1L, "user1", null,
        UserRole.REPORTER, Set.of(newChurch));

    assertThat(result.getAssignedChurches()).containsExactly(newChurch);
    assertThat(result.isAssignedToChurchName("Trinity")).isFalse();
    assertThat(result.isAssignedToChurchName("StPaul")).isTrue();
  }

  @Test
  void updateUser_notFound_throwsException() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.updateUser(99L, "x", "pass", UserRole.ADMIN, Set.of()))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("99");
  }

  // --- CRUD delegations ---

  @Test
  void findAll_delegatesToRepository() {
    DashboardUser user = buildUser(1L, "admin", UserRole.ADMIN);
    when(userRepository.findAll()).thenReturn(List.of(user));

    List<DashboardUser> result = userService.findAll();

    assertThat(result).hasSize(1);
    verify(userRepository).findAll();
  }

  @Test
  void existsById_returnsTrue_whenExists() {
    when(userRepository.existsById(1L)).thenReturn(true);

    assertThat(userService.existsById(1L)).isTrue();
    verify(userRepository).existsById(1L);
  }

  @Test
  void deleteById_delegatesToRepository() {
    userService.deleteById(1L);

    verify(userRepository).deleteById(1L);
  }

  @Test
  void existsByUsername_delegatesToRepository() {
    when(userRepository.existsByUsername("admin")).thenReturn(true);

    assertThat(userService.existsByUsername("admin")).isTrue();
    verify(userRepository).existsByUsername("admin");
  }

  @Test
  void existsByRole_delegatesToRepository() {
    when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);

    assertThat(userService.existsByRole(UserRole.ADMIN)).isTrue();
    verify(userRepository).existsByRole(UserRole.ADMIN);
  }
}
