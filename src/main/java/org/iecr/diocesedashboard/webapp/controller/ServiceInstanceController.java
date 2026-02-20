package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.service.ServiceInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
   * Returns all service instances.
   *
   * @return list of all service instances
   */
  @GetMapping
  public List<ServiceInstance> getAll() {
    return serviceInstanceService.findAll();
  }

  /**
   * Returns the service instance with the given ID.
   *
   * @param id the service instance ID
   * @return 200 with the instance, or 404 if not found
   */
  @GetMapping("/{id}")
  public ResponseEntity<ServiceInstance> getById(@PathVariable Long id) {
    return serviceInstanceService.findById(id)
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
}
