package org.iecr.diocesedashboard.domain.repositories;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChurchRepository extends JpaRepository<Church, String> {
  // ...basic CRUD methods provided by JpaRepository...
}
