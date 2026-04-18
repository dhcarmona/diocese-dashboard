package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.Valid;
import org.iecr.diocesedashboard.domain.objects.SectionHeader;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.service.SectionHeaderService;
import org.iecr.diocesedashboard.service.ServiceTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** REST controller for managing section headers within service templates. */
@RestController
@RequestMapping("/api/section-headers")
public class SectionHeaderController {

  private final SectionHeaderService sectionHeaderService;
  private final ServiceTemplateService serviceTemplateService;

  @Autowired
  public SectionHeaderController(SectionHeaderService sectionHeaderService,
      ServiceTemplateService serviceTemplateService) {
    this.sectionHeaderService = sectionHeaderService;
    this.serviceTemplateService = serviceTemplateService;
  }

  /**
   * Creates a new section header associated with the given template.
   * The header is appended at the end of the template's unified item list.
   *
   * @param templateId the ID of the owning service template
   * @param header     the header to create
   * @return 201 with the created header, or 404 if the template is not found
   */
  @PostMapping
  public ResponseEntity<SectionHeader> create(@RequestParam Long templateId,
      @RequestBody @Valid SectionHeader header) {
    ServiceTemplate template = serviceTemplateService.findById(templateId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "ServiceTemplate not found: " + templateId));
    header.setServiceTemplate(template);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(sectionHeaderService.createHeader(header));
  }

  /**
   * Updates the title of the section header with the given ID.
   *
   * @param id     the section header ID
   * @param header the updated header data
   * @return 200 with the updated header, or 404 if not found
   */
  @PutMapping("/{id}")
  public ResponseEntity<SectionHeader> update(@PathVariable Long id,
      @RequestBody @Valid SectionHeader header) {
    SectionHeader existing = sectionHeaderService.findById(id).orElse(null);
    if (existing == null) {
      return ResponseEntity.notFound().build();
    }
    existing.setTitle(header.getTitle());
    return ResponseEntity.ok(sectionHeaderService.save(existing));
  }

  /**
   * Deletes the section header with the given ID.
   *
   * @param id the section header ID
   * @return 204 on success, or 404 if not found
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    if (!sectionHeaderService.existsById(id)) {
      return ResponseEntity.notFound().build();
    }
    sectionHeaderService.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
