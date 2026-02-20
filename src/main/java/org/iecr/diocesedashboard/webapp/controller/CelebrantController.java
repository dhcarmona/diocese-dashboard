package org.iecr.diocesedashboard.webapp.controller;

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

@RestController
@RequestMapping("/api/celebrants")
public class CelebrantController {

  private final CelebrantService celebrantService;

  @Autowired
  public CelebrantController(CelebrantService celebrantService) {
    this.celebrantService = celebrantService;
  }

  @GetMapping
  public List<Celebrant> getAll() {
    return celebrantService.findAll();
  }

  @GetMapping("/{id}")
  public ResponseEntity<Celebrant> getById(@PathVariable Long id) {
    return celebrantService.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<Celebrant> create(@RequestBody Celebrant celebrant) {
    return ResponseEntity.status(HttpStatus.CREATED).body(celebrantService.save(celebrant));
  }

  @PutMapping("/{id}")
  public ResponseEntity<Celebrant> update(@PathVariable Long id, @RequestBody Celebrant celebrant) {
    if (!celebrantService.existsById(id)) {
      return ResponseEntity.notFound().build();
    }
    celebrant.setId(id);
    return ResponseEntity.ok(celebrantService.save(celebrant));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    if (!celebrantService.existsById(id)) {
      return ResponseEntity.notFound().build();
    }
    celebrantService.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
