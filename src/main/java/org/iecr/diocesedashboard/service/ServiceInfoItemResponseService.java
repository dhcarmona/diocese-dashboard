package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemResponse;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.repositories.ServiceInfoItemResponseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ServiceInfoItemResponseService {

  private final ServiceInfoItemResponseRepository serviceInfoItemResponseRepository;

  @Autowired
  public ServiceInfoItemResponseService(
      ServiceInfoItemResponseRepository serviceInfoItemResponseRepository) {
    this.serviceInfoItemResponseRepository = serviceInfoItemResponseRepository;
  }

  public List<ServiceInfoItemResponse> findAll() {
    return serviceInfoItemResponseRepository.findAll();
  }

  public List<ServiceInfoItemResponse> findByServiceInstance(ServiceInstance instance) {
    return serviceInfoItemResponseRepository.findByServiceInstance(instance);
  }

  public Optional<ServiceInfoItemResponse> findById(Long id) {
    return serviceInfoItemResponseRepository.findById(id);
  }

  public ServiceInfoItemResponse save(ServiceInfoItemResponse serviceInfoItemResponse) {
    return serviceInfoItemResponseRepository.save(serviceInfoItemResponse);
  }

  @Transactional
  public void deleteByServiceInstance(ServiceInstance instance) {
    serviceInfoItemResponseRepository.deleteByServiceInstance(instance);
  }

  public void deleteById(Long id) {
    serviceInfoItemResponseRepository.deleteById(id);
  }

  public boolean existsById(Long id) {
    return serviceInfoItemResponseRepository.existsById(id);
  }
}
