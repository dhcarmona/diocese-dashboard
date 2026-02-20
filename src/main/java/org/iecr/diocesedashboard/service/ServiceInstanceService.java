package org.iecr.diocesedashboard.service;

import java.util.List;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.repositories.ServiceInstanceRepository;
import org.springframework.stereotype.Service;

@Service
public class ServiceInstanceService {
  private final ServiceInstanceRepository repository;

  public ServiceInstanceService(ServiceInstanceRepository repository) {
    this.repository = repository;
  }

  public List<ServiceInstance> findAll() {
    return repository.findAll();
  }

  public ServiceInstance findById(Long id) {
    return repository.findById(id).orElse(null);
  }

  public ServiceInstance save(ServiceInstance instance) {
    return repository.save(instance);
  }

  public void deleteById(Long id) {
    repository.deleteById(id);
  }
}