package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.repositories.ServiceInfoItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ServiceInfoItemService {

  private final ServiceInfoItemRepository serviceInfoItemRepository;

  @Autowired
  public ServiceInfoItemService(ServiceInfoItemRepository serviceInfoItemRepository) {
    this.serviceInfoItemRepository = serviceInfoItemRepository;
  }

  public List<ServiceInfoItem> findAll() {
    return serviceInfoItemRepository.findAll();
  }

  public Optional<ServiceInfoItem> findById(Long id) {
    return serviceInfoItemRepository.findById(id);
  }

  public ServiceInfoItem save(ServiceInfoItem serviceInfoItem) {
    return serviceInfoItemRepository.save(serviceInfoItem);
  }

  /**
   * Persists a new ServiceInfoItem, automatically assigning it the next
   * sort order position at the end of its template's item list.
   *
   * @param serviceInfoItem the item to create (serviceTemplate must already be set)
   * @return the saved item with its assigned sortOrder
   */
  public ServiceInfoItem createItem(ServiceInfoItem serviceInfoItem) {
    Long templateId = serviceInfoItem.getServiceTemplate().getId();
    int nextOrder = serviceInfoItemRepository.findMaxSortOrderByTemplateId(templateId) + 1;
    serviceInfoItem.setSortOrder(nextOrder);
    return serviceInfoItemRepository.save(serviceInfoItem);
  }

  /**
   * Reassigns sortOrder for each item in {@code orderedIds} so the list
   * position matches the provided sequence. Runs in a single transaction.
   *
   * @param orderedIds item IDs in the desired display order
   */
  @Transactional
  public void reorder(List<Long> orderedIds) {
    for (int position = 0; position < orderedIds.size(); position++) {
      serviceInfoItemRepository.updateSortOrder(orderedIds.get(position), position);
    }
  }

  public void deleteById(Long id) {
    serviceInfoItemRepository.deleteById(id);
  }

  public boolean existsById(Long id) {
    return serviceInfoItemRepository.existsById(id);
  }
}
