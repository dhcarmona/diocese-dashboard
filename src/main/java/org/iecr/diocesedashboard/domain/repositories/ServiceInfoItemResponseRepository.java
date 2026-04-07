package org.iecr.diocesedashboard.domain.repositories;

import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemResponse;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceInfoItemResponseRepository
    extends JpaRepository<ServiceInfoItemResponse, Long> {

  List<ServiceInfoItemResponse> findByServiceInstance(ServiceInstance instance);

  void deleteByServiceInstance(ServiceInstance instance);
}
