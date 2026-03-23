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

import java.util.List;

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
    if (request.password() == null || request.password().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Password is required when creating a user");
    }
    Church church = resolveChurch(request.churchName());
    if (request.role() == UserRole.REPORTER && church == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "churchName is required for REPORTER role");
    }
    DashboardUser created = userService.createUser(
        request.username(), request.password(), request.role(), church);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
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
    Church church = resolveChurch(request.churchName());
    if (request.role() == UserRole.REPORTER && church == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "churchName is required for REPORTER role");
    }
    DashboardUser updated = userService.updateUser(
        id, request.username(), request.password(), request.role(), church);
    return ResponseEntity.ok(updated);
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

  private Church resolveChurch(String churchName) {
    if (churchName == null || churchName.isBlank()) {
      return null;
    }
    return churchService.findById(churchName)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Church not found: " + churchName));
  }
}
