package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.service.ServiceInstanceService;
import org.iecr.diocesedashboard.webapp.DashboardUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** REST controller for reading and deleting service instances. */
@RestController
@RequestMapping("/api/service-instances")
public class ServiceInstanceController {

  private final ServiceInstanceService serviceInstanceService;

  @Autowired
  public ServiceInstanceController(ServiceInstanceService serviceInstanceService) {
    this.serviceInstanceService = serviceInstanceService;
  }

  /**
   * Returns all service instances visible to the authenticated user.
   * REPORTER users only see instances for their assigned churches.
   *
   * @param auth the current authentication
   * @return list of visible service instances
   */
  @GetMapping
  public List<ServiceInstance> getAll(Authentication auth) {
    DashboardUser user = ((DashboardUserDetails) auth.getPrincipal()).getDashboardUser();
    if (user.getRole() == UserRole.REPORTER) {
      if (user.getAssignedChurches().isEmpty()) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Reporter account has no churches assigned");
      }
      return serviceInstanceService.findByChurches(user.getAssignedChurches());
    }
    return serviceInstanceService.findAll();
  }

  /**
   * Returns the service instance with the given ID.
   * REPORTER users receive 404 if the instance does not belong to their church.
   *
   * @param id   the service instance ID
   * @param auth the current authentication
   * @return 200 with the instance, or 404 if not found or not accessible
   */
  @GetMapping("/{id}")
  public ResponseEntity<ServiceInstance> getById(@PathVariable Long id, Authentication auth) {
    DashboardUser user = ((DashboardUserDetails) auth.getPrincipal()).getDashboardUser();
    return serviceInstanceService.findById(id)
        .filter(inst -> isAccessible(inst, user))
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Deletes the service instance with the given ID.
   *
   * @param id the service instance ID
   * @return 204 on success, or 404 if not found
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    if (!serviceInstanceService.existsById(id)) {
      return ResponseEntity.notFound().build();
    }
    serviceInstanceService.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  private boolean isAccessible(ServiceInstance instance, DashboardUser user) {
    if (user.getRole() == UserRole.ADMIN) {
      return true;
    }
    return user.isAssignedToChurch(instance.getChurch());
  }
}
