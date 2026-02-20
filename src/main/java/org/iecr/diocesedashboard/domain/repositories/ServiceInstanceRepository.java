package org.iecr.diocesedashboard.domain.repositories;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceInstanceRepository extends JpaRepository<ServiceInstance, Long> {

  List<ServiceInstance> findByChurch(Church church);
}
