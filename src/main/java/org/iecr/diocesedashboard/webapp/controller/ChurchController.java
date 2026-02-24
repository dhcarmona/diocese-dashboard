package org.iecr.diocesedashboard.webapp.controller;

import jakarta.validation.Valid;
import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.service.ChurchService;
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

/** REST controller for managing churches. */
@RestController
@RequestMapping("/api/churches")
public class ChurchController {

  private final ChurchService churchService;

  @Autowired
  public ChurchController(ChurchService churchService) {
    this.churchService = churchService;
  }

  /**
   * Returns all churches.
   *
   * @return list of all churches
   */
  @GetMapping
  public List<Church> getAll() {
    return churchService.findAll();
  }

  /**
   * Returns the church with the given name.
   *
   * @param name the church name (primary key)
   * @return 200 with the church, or 404 if not found
   */
  @GetMapping("/{name}")
  public ResponseEntity<Church> getByName(@PathVariable String name) {
    return churchService.findById(name)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Creates a new church.
   *
   * @param church the church to create
   * @return 201 with the created church
   */
  @PostMapping
  public ResponseEntity<Church> create(@RequestBody @Valid Church church) {
    return ResponseEntity.status(HttpStatus.CREATED).body(churchService.save(church));
  }

  /**
   * Updates the church with the given name.
   *
   * @param name   the church name (primary key)
   * @param church the updated church data
   * @return 200 with the updated church, or 404 if not found
   */
  @PutMapping("/{name}")
  public ResponseEntity<Church> update(@PathVariable String name, @RequestBody @Valid Church church) {
    if (!churchService.existsById(name)) {
      return ResponseEntity.notFound().build();
    }
    church.setName(name);
    return ResponseEntity.ok(churchService.save(church));
  }

  /**
   * Deletes the church with the given name.
   *
   * @param name the church name (primary key)
   * @return 204 on success, or 404 if not found
   */
  @DeleteMapping("/{name}")
  public ResponseEntity<Void> delete(@PathVariable String name) {
    if (!churchService.existsById(name)) {
      return ResponseEntity.notFound().build();
    }
    churchService.deleteById(name);
    return ResponseEntity.noContent().build();
  }
}
