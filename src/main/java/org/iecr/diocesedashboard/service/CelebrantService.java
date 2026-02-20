package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.Celebrant;
import org.iecr.diocesedashboard.domain.repositories.CelebrantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CelebrantService {

  private final CelebrantRepository celebrantRepository;

  @Autowired
  public CelebrantService(CelebrantRepository celebrantRepository) {
    this.celebrantRepository = celebrantRepository;
  }

  public List<Celebrant> findAll() {
    return celebrantRepository.findAll();
  }

  public Optional<Celebrant> findById(Long id) {
    return celebrantRepository.findById(id);
  }

  public Celebrant save(Celebrant celebrant) {
    return celebrantRepository.save(celebrant);
  }

  public void deleteById(Long id) {
    celebrantRepository.deleteById(id);
  }

  public boolean existsById(Long id) {
    return celebrantRepository.existsById(id);
  }
}
