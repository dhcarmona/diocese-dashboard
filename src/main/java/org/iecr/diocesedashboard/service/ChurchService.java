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

    @Autowired
    public ChurchService(ChurchRepository churchRepository) {
        this.churchRepository = churchRepository;
    }

    public List<Church> findAll() {
        return churchRepository.findAll();
    }

    public Optional<Church> findById(String name) {
        return churchRepository.findById(name);
    }

    public Church save(Church church) {
        return churchRepository.save(church);
    }

    public void deleteById(String name) {
        churchRepository.deleteById(name);
    }

    public boolean existsById(String name) {
        return churchRepository.existsById(name);
    }
}
