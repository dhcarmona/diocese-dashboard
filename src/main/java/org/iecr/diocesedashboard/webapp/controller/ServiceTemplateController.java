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

/** REST controller for managing service templates and submitting service instances. */
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

  /**
   * Returns all service templates.
   *
   * @return list of all service templates
   */
  @GetMapping
  public List<ServiceTemplate> getAll() {
    return serviceTemplateService.findAll();
  }

  /**
   * Returns the service template with the given ID.
   *
   * @param id the template ID
   * @return 200 with the template, or 404 if not found
   */
  @GetMapping("/{id}")
  public ResponseEntity<ServiceTemplate> getById(@PathVariable Long id) {
    return serviceTemplateService.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Creates a new service template.
   *
   * @param template the template to create
   * @return 201 with the created template
   */
  @PostMapping
  public ResponseEntity<ServiceTemplate> create(@RequestBody ServiceTemplate template) {
    return ResponseEntity.status(HttpStatus.CREATED).body(serviceTemplateService.save(template));
  }

  /**
   * Updates the service template with the given ID.
   *
   * @param id       the template ID
   * @param template the updated template data
   * @return 200 with the updated template, or 404 if not found
   */
  @PutMapping("/{id}")
  public ResponseEntity<ServiceTemplate> update(@PathVariable Long id,
      @RequestBody ServiceTemplate template) {
    if (!serviceTemplateService.existsById(id)) {
      return ResponseEntity.notFound().build();
    }
    template.setId(id);
    return ResponseEntity.ok(serviceTemplateService.save(template));
  }

  /**
   * Deletes the service template with the given ID.
   *
   * @param id the template ID
   * @return 204 on success, or 404 if not found
   */
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
   *
   * @param id      the template ID
   * @param request the submission request body
   * @return 201 with the created service instance
   */
  @PostMapping("/{id}/submit")
  public ResponseEntity<ServiceInstance> submit(@PathVariable Long id,
      @RequestBody ServiceInstanceRequest request) {
    ServiceInstance created = serviceSubmissionService.submit(id, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }
}
