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

  public void deleteById(Long id) {
    serviceInfoItemRepository.deleteById(id);
  }

  public boolean existsById(Long id) {
    return serviceInfoItemRepository.existsById(id);
  }
}
