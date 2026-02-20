package org.iecr.diocesedashboard.domain.repositories;

import org.iecr.diocesedashboard.domain.objects.Celebrant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CelebrantRepository extends JpaRepository<Celebrant, Long> {
    // ...basic CRUD methods provided by JpaRepository...
}

