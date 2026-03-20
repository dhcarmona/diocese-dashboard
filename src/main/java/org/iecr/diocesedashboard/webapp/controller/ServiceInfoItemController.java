package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.Valid;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.service.ServiceInfoItemService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** REST controller for managing service info items (survey questions on a template). */
@RestController
@RequestMapping("/api/service-info-items")
public class ServiceInfoItemController {

  private final ServiceInfoItemService serviceInfoItemService;
  private final ServiceTemplateService serviceTemplateService;

  @Autowired
  public ServiceInfoItemController(ServiceInfoItemService serviceInfoItemService,
      ServiceTemplateService serviceTemplateService) {
    this.serviceInfoItemService = serviceInfoItemService;
    this.serviceTemplateService = serviceTemplateService;
  }

  /**
   * Returns all service info items.
   *
   * @return list of all service info items
   */
  @GetMapping
  public List<ServiceInfoItem> getAll() {
    return serviceInfoItemService.findAll();
  }

  /**
   * Returns the service info item with the given ID.
   *
   * @param id the service info item ID
   * @return 200 with the item, or 404 if not found
   */
  @GetMapping("/{id}")
  public ResponseEntity<ServiceInfoItem> getById(@PathVariable Long id) {
    return serviceInfoItemService.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Creates a new service info item, associated with the given template.
   *
   * @param templateId the ID of the owning service template
   * @param item the item to create
   * @return 201 with the created item, or 404 if the template is not found
   */
  @PostMapping
  public ResponseEntity<ServiceInfoItem> create(@RequestParam Long templateId,
      @RequestBody @Valid ServiceInfoItem item) {
    ServiceTemplate template = serviceTemplateService.findById(templateId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "ServiceTemplate not found: " + templateId));
    item.setServiceTemplate(template);
    return ResponseEntity.status(HttpStatus.CREATED).body(serviceInfoItemService.save(item));
  }

  /**
   * Updates the service info item with the given ID.
   *
   * @param id         the service info item ID
   * @param templateId the ID of the owning service template
   * @param item       the updated item data
   * @return 200 with the updated item, or 404 if not found
   */
  @PutMapping("/{id}")
  public ResponseEntity<ServiceInfoItem> update(@PathVariable Long id,
      @RequestParam Long templateId,
      @RequestBody @Valid ServiceInfoItem item) {
    if (!serviceInfoItemService.existsById(id)) {
      return ResponseEntity.notFound().build();
    }
    ServiceTemplate template = serviceTemplateService.findById(templateId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "ServiceTemplate not found: " + templateId));
    item.setId(id);
    item.setServiceTemplate(template);
    return ResponseEntity.ok(serviceInfoItemService.save(item));
  }

  /**
   * Deletes the service info item with the given ID.
   *
   * @param id the service info item ID
   * @return 204 on success, or 404 if not found
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    if (!serviceInfoItemService.existsById(id)) {
      return ResponseEntity.notFound().build();
    }
    serviceInfoItemService.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
