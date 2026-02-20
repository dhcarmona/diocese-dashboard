package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.service.ServiceSubmissionService;
import org.iecr.diocesedashboard.service.ServiceTemplateService;
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

import java.util.List;

@RestController
@RequestMapping("/api/service-templates")
public class ServiceTemplateController {

  private final ServiceTemplateService serviceTemplateService;
  private final ServiceSubmissionService serviceSubmissionService;

  @Autowired
  public ServiceTemplateController(ServiceTemplateService serviceTemplateService,
      ServiceSubmissionService serviceSubmissionService) {
    this.serviceTemplateService = serviceTemplateService;
    this.serviceSubmissionService = serviceSubmissionService;
  }

  @GetMapping
  public List<ServiceTemplate> getAll() {
    return serviceTemplateService.findAll();
  }

  @GetMapping("/{id}")
  public ResponseEntity<ServiceTemplate> getById(@PathVariable Long id) {
    return serviceTemplateService.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<ServiceTemplate> create(@RequestBody ServiceTemplate template) {
    return ResponseEntity.status(HttpStatus.CREATED).body(serviceTemplateService.save(template));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ServiceTemplate> update(@PathVariable Long id,
      @RequestBody ServiceTemplate template) {
    if (!serviceTemplateService.existsById(id)) {
      return ResponseEntity.notFound().build();
    }
    template.setId(id);
    return ResponseEntity.ok(serviceTemplateService.save(template));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    if (!serviceTemplateService.existsById(id)) {
      return ResponseEntity.notFound().build();
    }
    serviceTemplateService.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * The unique URL for each Service Template. Regular users navigate to this endpoint
   * to submit a new Service Instance for the given template.
   */
  @PostMapping("/{id}/submit")
  public ResponseEntity<ServiceInstance> submit(@PathVariable Long id,
      @RequestBody ServiceInstanceRequest request) {
    ServiceInstance created = serviceSubmissionService.submit(id, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }
}
