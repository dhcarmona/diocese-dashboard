package org.iecr.diocesedashboard.domain.repositories;

import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemResponse;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceInfoItemResponseRepository
    extends JpaRepository<ServiceInfoItemResponse, Long> {

  List<ServiceInfoItemResponse> findByServiceInstance(ServiceInstance instance);

  List<ServiceInfoItemResponse> findByServiceInstanceIn(List<ServiceInstance> instances);

  @Query("SELECT r FROM ServiceInfoItemResponse r "
      + "JOIN FETCH r.serviceInfoItem "
      + "WHERE r.serviceInstance IN :instances")
  List<ServiceInfoItemResponse> findByServiceInstanceInWithItems(
      @Param("instances") List<ServiceInstance> instances);

  void deleteByServiceInstance(ServiceInstance instance);
}
