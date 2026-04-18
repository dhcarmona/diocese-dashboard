package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.domain.repositories.UserRepository;
import org.iecr.diocesedashboard.webapp.DashboardUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Map;
import java.util.Set;

/** Service for managing dashboard user accounts and Spring Security authentication. */
@Service
public class UserService implements UserDetailsService {

  private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final WhatsAppService whatsAppService;
  private final MessageSource messageSource;

  @Autowired
  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
      WhatsAppService whatsAppService, MessageSource messageSource) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.whatsAppService = whatsAppService;
    this.messageSource = messageSource;
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
   * Convenience overload for creating a user without {@code fullName}, {@code phoneNumber},
   * or {@code appBaseUrl}. Delegates to
   * {@link #createUser(String, String, UserRole, Set, String, String, String)}.
   * Primarily used by the bootstrap initializer for ADMIN accounts; for REPORTER accounts
   * prefer the overload that accepts {@code fullName}, {@code phoneNumber}, and
   * {@code appBaseUrl}.
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
    return createUser(username, rawPassword, role, assignedChurches, null, null, null);
  }

  /**
   * Creates and persists a new user account.
   * For ADMIN users, {@code rawPassword} is encoded and stored.
   * For REPORTER users, pass {@code null} for {@code rawPassword}; provide fullName,
   * phoneNumber, and appBaseUrl instead. A welcome WhatsApp message is sent to the reporter
   * if phoneNumber and appBaseUrl are provided.
   *
   * @param username         the unique username
   * @param rawPassword      plain-text password (ADMIN only; null for REPORTER)
   * @param role             the user's role
   * @param assignedChurches the churches to assign
   * @param fullName         the reporter's full name (REPORTER only)
   * @param phoneNumber      the reporter's phone number in E.164 format (REPORTER only)
   * @param appBaseUrl       the application base URL included in the welcome message
   *                         (REPORTER only; null to skip the welcome message)
   * @return the saved {@link DashboardUser}
   */
  public DashboardUser createUser(String username, String rawPassword,
      UserRole role, Set<Church> assignedChurches, String fullName, String phoneNumber,
      String appBaseUrl) {
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
    DashboardUser saved = userRepository.save(user);
    if (role == UserRole.REPORTER && phoneNumber != null && !phoneNumber.isBlank()
        && appBaseUrl != null) {
      try {
        String body = messageSource.getMessage(
            "reporter.welcome.whatsapp.message",
            new Object[]{fullName, username, appBaseUrl},
            saved.getPreferredLocale());
        whatsAppService.sendConfiguredMessageAndLog(
            phoneNumber,
            body,
            username,
            WhatsAppService.TemplateType.REPORTER_WELCOME,
            buildTemplateVariables(fullName, username, appBaseUrl));
      } catch (Exception ex) {
        LOG.warn("Failed to send welcome WhatsApp to new reporter '{}'",
            username, ex);
      }
    }
    return saved;
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

  /**
   * Updates the preferred UI and WhatsApp language for an existing user.
   *
   * @param id       the ID of the user to update
   * @param language the requested language code
   * @return the saved {@link DashboardUser}
   * @throws UsernameNotFoundException if no user with the given ID exists
   */
  public DashboardUser updatePreferredLanguage(Long id, String language) {
    DashboardUser user = userRepository.findById(id)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));
    user.setPreferredLanguage(language);
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

  private Map<String, String> buildTemplateVariables(String fullName, String username,
      String appBaseUrl) {
    Map<String, String> templateVariables = new LinkedHashMap<>();
    templateVariables.put("1", fullName == null ? "" : fullName);
    templateVariables.put("2", username);
    templateVariables.put("3", appBaseUrl);
    return templateVariables;
  }
}
