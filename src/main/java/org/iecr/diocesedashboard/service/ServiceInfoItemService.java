package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.repositories.ServiceInfoItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ServiceInfoItemService {

  private final ServiceInfoItemRepository serviceInfoItemRepository;
  private final TemplateItemOrderService templateItemOrderService;

  @Autowired
  public ServiceInfoItemService(ServiceInfoItemRepository serviceInfoItemRepository,
      TemplateItemOrderService templateItemOrderService) {
    this.serviceInfoItemRepository = serviceInfoItemRepository;
    this.templateItemOrderService = templateItemOrderService;
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
   * Persists a new ServiceInfoItem, automatically assigning it the next sort order position
   * at the end of its template's unified item list (info items + section headers).
   *
   * @param serviceInfoItem the item to create (serviceTemplate must already be set)
   * @return the saved item with its assigned sortOrder
   */
  public ServiceInfoItem createItem(ServiceInfoItem serviceInfoItem) {
    Long templateId = serviceInfoItem.getServiceTemplate().getId();
    int nextOrder = templateItemOrderService.getNextSortOrder(templateId);
    serviceInfoItem.setSortOrder(nextOrder);
    return serviceInfoItemRepository.save(serviceInfoItem);
  }

  public void deleteById(Long id) {
    serviceInfoItemRepository.deleteById(id);
  }

  public boolean existsById(Long id) {
    return serviceInfoItemRepository.existsById(id);
  }
}
