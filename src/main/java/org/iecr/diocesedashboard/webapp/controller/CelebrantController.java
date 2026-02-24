package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.Valid;
import org.iecr.diocesedashboard.domain.objects.Celebrant;
import org.iecr.diocesedashboard.service.CelebrantService;
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

/** REST controller for managing celebrants (clergy members). */
@RestController
@RequestMapping("/api/celebrants")
public class CelebrantController {

  private final CelebrantService celebrantService;

  @Autowired
  public CelebrantController(CelebrantService celebrantService) {
    this.celebrantService = celebrantService;
  }

  /**
   * Returns all celebrants.
   *
   * @return list of all celebrants
   */
  @GetMapping
  public List<Celebrant> getAll() {
    return celebrantService.findAll();
  }

  /**
   * Returns the celebrant with the given ID.
   *
   * @param id the celebrant ID
   * @return 200 with the celebrant, or 404 if not found
   */
  @GetMapping("/{id}")
  public ResponseEntity<Celebrant> getById(@PathVariable Long id) {
    return celebrantService.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Creates a new celebrant.
   *
   * @param celebrant the celebrant to create
   * @return 201 with the created celebrant
   */
  @PostMapping
  public ResponseEntity<Celebrant> create(@RequestBody @Valid Celebrant celebrant) {
    return ResponseEntity.status(HttpStatus.CREATED).body(celebrantService.save(celebrant));
  }

  /**
   * Updates the celebrant with the given ID.
   *
   * @param id        the celebrant ID
   * @param celebrant the updated celebrant data
   * @return 200 with the updated celebrant, or 404 if not found
   */
  @PutMapping("/{id}")
  public ResponseEntity<Celebrant> update(@PathVariable Long id, @RequestBody @Valid Celebrant celebrant) {
    if (!celebrantService.existsById(id)) {
      return ResponseEntity.notFound().build();
    }
    celebrant.setId(id);
    return ResponseEntity.ok(celebrantService.save(celebrant));
  }

  /**
   * Deletes the celebrant with the given ID.
   *
   * @param id the celebrant ID
   * @return 204 on success, or 404 if not found
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    if (!celebrantService.existsById(id)) {
      return ResponseEntity.notFound().build();
    }
    celebrantService.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
