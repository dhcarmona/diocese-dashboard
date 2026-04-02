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
  private final PortraitService portraitService;

  @Autowired
  public CelebrantService(CelebrantRepository celebrantRepository, PortraitService portraitService) {
    this.celebrantRepository = celebrantRepository;
    this.portraitService = portraitService;
  }

  public List<Celebrant> findAll() {
    return celebrantRepository.findAll().stream()
        .map(this::attachPortrait)
        .toList();
  }

  public Optional<Celebrant> findById(Long id) {
    return celebrantRepository.findById(id).map(this::attachPortrait);
  }

  public Celebrant save(Celebrant celebrant) {
    return attachPortrait(celebrantRepository.save(celebrant));
  }

  public void deleteById(Long id) {
    celebrantRepository.deleteById(id);
  }

  public boolean existsById(Long id) {
    return celebrantRepository.existsById(id);
  }

  private Celebrant attachPortrait(Celebrant celebrant) {
    celebrant.setPortraitUrl(portraitService.buildCelebrantPortraitUrl(celebrant.getName()));
    return celebrant;
  }
}
