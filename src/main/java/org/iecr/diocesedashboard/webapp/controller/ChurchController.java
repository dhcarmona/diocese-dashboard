package org.iecr.diocesedashboard.webapp.controller;

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

@RestController
@RequestMapping("/api/churches")
public class ChurchController {

  private final ChurchService churchService;

  @Autowired
  public ChurchController(ChurchService churchService) {
    this.churchService = churchService;
  }

  @GetMapping
  public List<Church> getAll() {
    return churchService.findAll();
  }

  @GetMapping("/{name}")
  public ResponseEntity<Church> getByName(@PathVariable String name) {
    return churchService.findById(name)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<Church> create(@RequestBody Church church) {
    return ResponseEntity.status(HttpStatus.CREATED).body(churchService.save(church));
  }

  @PutMapping("/{name}")
  public ResponseEntity<Church> update(@PathVariable String name, @RequestBody Church church) {
    if (!churchService.existsById(name)) {
      return ResponseEntity.notFound().build();
    }
    church.setName(name);
    return ResponseEntity.ok(churchService.save(church));
  }

  @DeleteMapping("/{name}")
  public ResponseEntity<Void> delete(@PathVariable String name) {
    if (!churchService.existsById(name)) {
      return ResponseEntity.notFound().build();
    }
    churchService.deleteById(name);
    return ResponseEntity.noContent().build();
  }
}
