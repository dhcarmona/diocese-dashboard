package org.iecr.diocesedashboard.domain.repositories;

import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceTemplateRepository extends JpaRepository<ServiceTemplate, Long> {
  // ...basic CRUD methods provided by JpaRepository...
}
