package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.repositories.ChurchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ChurchService {

  private final ChurchRepository churchRepository;
  private final PortraitService portraitService;

  @Autowired
  public ChurchService(ChurchRepository churchRepository, PortraitService portraitService) {
    this.churchRepository = churchRepository;
    this.portraitService = portraitService;
  }

  public List<Church> findAll() {
    return churchRepository.findAll().stream()
        .map(this::attachPortrait)
        .toList();
  }

  public Optional<Church> findById(String name) {
    return churchRepository.findById(name).map(this::attachPortrait);
  }

  /**
   * Fetches all churches whose names are in the given collection, in a single query.
   *
   * @param names the church names to look up
   * @return a list of matching churches with portrait URLs attached
   */
  public List<Church> findAllById(Iterable<String> names) {
    return churchRepository.findAllById(names).stream()
        .map(this::attachPortrait)
        .toList();
  }

  public Church save(Church church) {
    return attachPortrait(churchRepository.save(church));
  }

  public void deleteById(String name) {
    churchRepository.deleteById(name);
  }

  public boolean existsById(String name) {
    return churchRepository.existsById(name);
  }

  private Church attachPortrait(Church church) {
    church.setPortraitUrl(portraitService.buildChurchPortraitUrl(church.getName()));
    return church;
  }
}
