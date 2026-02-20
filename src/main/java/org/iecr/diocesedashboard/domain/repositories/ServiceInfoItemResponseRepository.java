package org.iecr.diocesedashboard.domain.repositories;

import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceInfoItemResponseRepository extends JpaRepository<ServiceInfoItemResponse, Long> {
  // ...basic CRUD methods provided by JpaRepository...
}
