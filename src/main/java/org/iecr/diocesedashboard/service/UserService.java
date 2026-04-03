package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.domain.repositories.UserRepository;
import org.iecr.diocesedashboard.webapp.DashboardUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Service for managing dashboard user accounts and Spring Security authentication. */
@Service
public class UserService implements UserDetailsService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Autowired
  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Loads a user by username for Spring Security authentication.
   *
   * @param username the username to look up
   * @return the matching {@link UserDetails}
   * @throws UsernameNotFoundException if no user with that username exists
   */
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    DashboardUser user = userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    return new DashboardUserDetails(user);
  }

  /**
   * Convenience overload for creating a user without {@code fullName} or {@code phoneNumber}.
   * Delegates to {@link #createUser(String, String, UserRole, Set, String, String)}.
   * Primarily used by the bootstrap initializer for ADMIN accounts; for REPORTER accounts
   * prefer the 6-argument overload that accepts {@code fullName} and {@code phoneNumber}.
   *
   * @param username         the unique username
   * @param rawPassword      plain-text password (required for ADMIN; pass {@code null} for
   *                         REPORTER)
   * @param role             the user's role
   * @param assignedChurches the churches to assign
   * @return the saved {@link DashboardUser}
   */
  public DashboardUser createUser(String username, String rawPassword,
      UserRole role, Set<Church> assignedChurches) {
    return createUser(username, rawPassword, role, assignedChurches, null, null);
  }

  /**
   * Creates and persists a new user account.
   * For ADMIN users, {@code rawPassword} is encoded and stored.
   * For REPORTER users, pass {@code null} for {@code rawPassword}; provide fullName and
   * phoneNumber instead.
   *
   * @param username         the unique username
   * @param rawPassword      plain-text password (ADMIN only; null for REPORTER)
   * @param role             the user's role
   * @param assignedChurches the churches to assign
   * @param fullName         the reporter's full name (REPORTER only)
   * @param phoneNumber      the reporter's phone number in E.164 format (REPORTER only)
   * @return the saved {@link DashboardUser}
   */
  public DashboardUser createUser(String username, String rawPassword,
      UserRole role, Set<Church> assignedChurches, String fullName, String phoneNumber) {
    DashboardUser user = new DashboardUser();
    user.setUsername(username);
    if (rawPassword != null && !rawPassword.isBlank()) {
      user.setPasswordHash(passwordEncoder.encode(rawPassword));
    }
    user.setRole(role);
    user.setAssignedChurches(assignedChurches);
    user.setFullName(fullName);
    user.setPhoneNumber(phoneNumber);
    user.setEnabled(true);
    return userRepository.save(user);
  }

  /**
   * Updates an existing user. Re-encodes the password only if a new one is provided.
   * For REPORTER users, {@code rawPassword} should be null; fullName and phoneNumber are updated.
   *
   * @param id          the ID of the user to update
   * @param username    the new username
   * @param rawPassword the new plain-text password, or null/blank to keep the existing one
   * @param role        the new role
   * @param assignedChurches the new church assignments (empty for ADMIN)
   * @param fullName    the reporter's full name (REPORTER only; null for ADMIN)
   * @param phoneNumber the reporter's phone number (REPORTER only; null for ADMIN)
   * @return the updated {@link DashboardUser}
   * @throws UsernameNotFoundException if no user with the given ID exists
   */
  public DashboardUser updateUser(Long id, String username, String rawPassword,
      UserRole role, Set<Church> assignedChurches, String fullName, String phoneNumber) {
    DashboardUser user = userRepository.findById(id)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));
    user.setUsername(username);
    user.setRole(role);
    user.setAssignedChurches(assignedChurches);
    user.setFullName(fullName);
    user.setPhoneNumber(phoneNumber);
    if (rawPassword != null && !rawPassword.isBlank()) {
      user.setPasswordHash(passwordEncoder.encode(rawPassword));
    }
    return userRepository.save(user);
  }

  public List<DashboardUser> findAll() {
    return userRepository.findAll();
  }

  public Optional<DashboardUser> findById(Long id) {
    return userRepository.findById(id);
  }

  public Optional<DashboardUser> findByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  public void deleteById(Long id) {
    userRepository.deleteById(id);
  }

  public boolean existsById(Long id) {
    return userRepository.existsById(id);
  }

  public boolean existsByUsername(String username) {
    return userRepository.existsByUsername(username);
  }

  public boolean existsByRole(UserRole role) {
    return userRepository.existsByRole(role);
  }
}
