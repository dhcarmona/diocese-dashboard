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

@RestController
@RequestMapping("/api/service-instances")
public class ServiceInstanceController {

  private final ServiceInstanceService serviceInstanceService;

  @Autowired
  public ServiceInstanceController(ServiceInstanceService serviceInstanceService) {
    this.serviceInstanceService = serviceInstanceService;
  }

  @GetMapping
  public List<ServiceInstance> getAll() {
    return serviceInstanceService.findAll();
  }

  @GetMapping("/{id}")
  public ResponseEntity<ServiceInstance> getById(@PathVariable Long id) {
    return serviceInstanceService.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    if (!serviceInstanceService.existsById(id)) {
      return ResponseEntity.notFound().build();
    }
    serviceInstanceService.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
