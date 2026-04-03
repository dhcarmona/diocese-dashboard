package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.Valid;
import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.service.ChurchService;
import org.iecr.diocesedashboard.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** REST controller for managing user accounts (ADMIN only). */
@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;
  private final ChurchService churchService;

  @Autowired
  public UserController(UserService userService, ChurchService churchService) {
    this.userService = userService;
    this.churchService = churchService;
  }

  /**
   * Returns all user accounts.
   *
   * @return list of all dashboard users
   */
  @GetMapping
  public List<DashboardUser> getAll() {
    return userService.findAll();
  }

  /**
   * Returns the user account with the given ID.
   *
   * @param id the user ID
   * @return 200 with the user, or 404 if not found
   */
  @GetMapping("/{id}")
  public ResponseEntity<DashboardUser> getById(@PathVariable Long id) {
    return userService.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Creates a new user account.
   *
   * @param request the user creation request
   * @return 201 with the created user
   */
  @PostMapping
  public ResponseEntity<DashboardUser> create(@RequestBody @Valid UserRequest request) {
    Set<Church> churches = resolveChurches(request.churchNames());
    if (request.role() == UserRole.ADMIN) {
      if (request.password() == null || request.password().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Password is required when creating an ADMIN user");
      }
      churches = Set.of();
      DashboardUser created = userService.createUser(
          request.username(), request.password(), UserRole.ADMIN, churches, null, null);
      return ResponseEntity.status(HttpStatus.CREATED).body(created);
    } else {
      if (churches.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "churchNames is required for REPORTER role");
      }
      if (request.fullName() == null || request.fullName().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "fullName is required for REPORTER role");
      }
      if (request.phoneNumber() == null || request.phoneNumber().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "phoneNumber is required for REPORTER role");
      }
      DashboardUser created = userService.createUser(
          request.username(), null, UserRole.REPORTER, churches,
          request.fullName().trim(), request.phoneNumber().trim());
      return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
  }

  /**
   * Updates the user account with the given ID.
   *
   * @param id      the user ID
   * @param request the updated user data
   * @return 200 with the updated user, or 404 if not found
   */
  @PutMapping("/{id}")
  public ResponseEntity<DashboardUser> update(@PathVariable Long id,
      @RequestBody @Valid UserRequest request) {
    if (!userService.existsById(id)) {
      return ResponseEntity.notFound().build();
    }
    Set<Church> churches = resolveChurches(request.churchNames());
    if (request.role() == UserRole.ADMIN) {
      churches = Set.of();
      DashboardUser updated = userService.updateUser(
          id, request.username(), request.password(), UserRole.ADMIN, churches, null, null);
      return ResponseEntity.ok(updated);
    } else {
      if (churches.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "churchNames is required for REPORTER role");
      }
      if (request.fullName() == null || request.fullName().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "fullName is required for REPORTER role");
      }
      if (request.phoneNumber() == null || request.phoneNumber().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "phoneNumber is required for REPORTER role");
      }
      DashboardUser updated = userService.updateUser(
          id, request.username(), null, UserRole.REPORTER, churches,
          request.fullName().trim(), request.phoneNumber().trim());
      return ResponseEntity.ok(updated);
    }
  }

  /**
   * Deletes the user account with the given ID.
   *
   * @param id the user ID
   * @return 204 on success, or 404 if not found
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    if (!userService.existsById(id)) {
      return ResponseEntity.notFound().build();
    }
    userService.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  private Set<Church> resolveChurches(Set<String> churchNames) {
    Set<Church> churches = new LinkedHashSet<>();
    if (churchNames == null) {
      return churches;
    }
    for (String churchName : churchNames) {
      if (churchName == null || churchName.isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "churchNames may not contain blank values");
      }
      Church church = churchService.findById(churchName)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "Church not found: " + churchName));
      churches.add(church);
    }
    return churches;
  }
}
