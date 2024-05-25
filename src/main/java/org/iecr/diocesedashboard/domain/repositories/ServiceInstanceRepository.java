package org.iecr.diocesedashboard.domain.repositories;

import java.util.List;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceInstanceRepository extends JpaRepository<ServiceInstance, Long> {

  List<ServiceInstance> findBy(String title);
}
