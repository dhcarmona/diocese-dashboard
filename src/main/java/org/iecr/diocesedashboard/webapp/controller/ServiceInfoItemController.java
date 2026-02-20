package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.service.ServiceInfoItemService;
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
@RequestMapping("/api/service-info-items")
public class ServiceInfoItemController {

  private final ServiceInfoItemService serviceInfoItemService;

  @Autowired
  public ServiceInfoItemController(ServiceInfoItemService serviceInfoItemService) {
    this.serviceInfoItemService = serviceInfoItemService;
  }

  @GetMapping
  public List<ServiceInfoItem> getAll() {
    return serviceInfoItemService.findAll();
  }

  @GetMapping("/{id}")
  public ResponseEntity<ServiceInfoItem> getById(@PathVariable Long id) {
    return serviceInfoItemService.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<ServiceInfoItem> create(@RequestBody ServiceInfoItem item) {
    return ResponseEntity.status(HttpStatus.CREATED).body(serviceInfoItemService.save(item));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ServiceInfoItem> update(@PathVariable Long id,
      @RequestBody ServiceInfoItem item) {
    if (!serviceInfoItemService.existsById(id)) {
      return ResponseEntity.notFound().build();
    }
    item.setId(id);
    return ResponseEntity.ok(serviceInfoItemService.save(item));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    if (!serviceInfoItemService.existsById(id)) {
      return ResponseEntity.notFound().build();
    }
    serviceInfoItemService.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
