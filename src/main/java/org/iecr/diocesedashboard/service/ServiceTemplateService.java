package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.repositories.ServiceTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ServiceTemplateService {

    private final ServiceTemplateRepository repository;

    @Autowired
    public ServiceTemplateService(ServiceTemplateRepository repository) {
        this.repository = repository;
    }

    public List<ServiceTemplate> findAll() {
        return repository.findAll();
    }

    public Optional<ServiceTemplate> findById(Long id) {
        return repository.findById(id);
    }

    public ServiceTemplate save(ServiceTemplate serviceTemplate) {
        return repository.save(serviceTemplate);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public boolean existsById(Long id) {
        return repository.existsById(id);
    }
}
