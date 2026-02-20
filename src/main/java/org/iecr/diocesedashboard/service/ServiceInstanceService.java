package org.iecr.diocesedashboard.service;

import java.util.List;
import java.util.Optional;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.repositories.ServiceInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServiceInstanceService {
  private final ServiceInstanceRepository repository;

  @Autowired
  public ServiceInstanceService(ServiceInstanceRepository repository) {
    this.repository = repository;
  }

  public List<ServiceInstance> findAll() {
    return repository.findAll();
  }

  public Optional<ServiceInstance> findById(Long id) {
    return repository.findById(id);
  }

  public ServiceInstance save(ServiceInstance instance) {
    return repository.save(instance);
  }

  public void deleteById(Long id) {
    repository.deleteById(id);
  }

  public boolean existsById(Long id) {
    return repository.existsById(id);
  }
}
