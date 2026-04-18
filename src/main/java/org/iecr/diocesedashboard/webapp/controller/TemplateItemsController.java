package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.Valid;
import org.iecr.diocesedashboard.service.TemplateItemOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for operations that span multiple template item types
 * (service info items and section headers) within a service template.
 */
@RestController
@RequestMapping("/api/template-items")
public class TemplateItemsController {

  private final TemplateItemOrderService templateItemOrderService;

  @Autowired
  public TemplateItemsController(TemplateItemOrderService templateItemOrderService) {
    this.templateItemOrderService = templateItemOrderService;
  }

  /**
   * Reorders all template items (info items and section headers) in a single operation.
   * The items are assigned sortOrder values matching their position in
   * {@code request.items()}.
   *
   * @param request the reorder request containing typed item references in the desired order
   * @return 204 on success
   */
  @PutMapping("/reorder")
  public ResponseEntity<Void> reorder(@RequestBody @Valid TemplateItemReorderRequest request) {
    templateItemOrderService.reorder(request.templateId(), request.items());
    return ResponseEntity.noContent().build();
  }
}
