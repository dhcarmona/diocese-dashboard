package org.iecr.diocesedashboard.domain.repositories;

import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceInfoItemRepository extends JpaRepository<ServiceInfoItem, Long> {
    // ...basic CRUD methods provided by JpaRepository...
}
