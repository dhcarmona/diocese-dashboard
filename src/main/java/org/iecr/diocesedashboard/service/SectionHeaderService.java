package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.SectionHeader;
import org.iecr.diocesedashboard.domain.repositories.SectionHeaderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Service for managing {@link SectionHeader} entities. */
@Service
public class SectionHeaderService {

  private final SectionHeaderRepository sectionHeaderRepository;
  private final TemplateItemOrderService templateItemOrderService;

  @Autowired
  public SectionHeaderService(SectionHeaderRepository sectionHeaderRepository,
      TemplateItemOrderService templateItemOrderService) {
    this.sectionHeaderRepository = sectionHeaderRepository;
    this.templateItemOrderService = templateItemOrderService;
  }

  public Optional<SectionHeader> findById(Long id) {
    return sectionHeaderRepository.findById(id);
  }

  public SectionHeader save(SectionHeader header) {
    return sectionHeaderRepository.save(header);
  }

  /**
   * Persists a new SectionHeader, automatically assigning it the next sort order position
   * at the end of the template's unified item list (info items + section headers).
   *
   * @param header the header to create (serviceTemplate must already be set)
   * @return the saved header with its assigned sortOrder
   */
  public SectionHeader createHeader(SectionHeader header) {
    Long templateId = header.getServiceTemplate().getId();
    int nextOrder = templateItemOrderService.getNextSortOrder(templateId);
    header.setSortOrder(nextOrder);
    return sectionHeaderRepository.save(header);
  }

  public void deleteById(Long id) {
    sectionHeaderRepository.deleteById(id);
  }

  public boolean existsById(Long id) {
    return sectionHeaderRepository.existsById(id);
  }
}
