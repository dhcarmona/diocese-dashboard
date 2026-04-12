package org.iecr.diocesedashboard.domain.repositories;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ServiceInstanceRepository extends JpaRepository<ServiceInstance, Long> {

  @Query("SELECT DISTINCT i FROM ServiceInstance i LEFT JOIN FETCH i.celebrants WHERE i.id = :id")
  Optional<ServiceInstance> findByIdWithCelebrants(@Param("id") Long id);

  List<ServiceInstance> findByChurch(Church church);

  List<ServiceInstance> findByChurchIn(Iterable<Church> churches);

  List<ServiceInstance> findByServiceTemplate(ServiceTemplate template);

  List<ServiceInstance> findByServiceTemplateAndChurchAndServiceDateBetween(
      ServiceTemplate template, Church church, LocalDate start, LocalDate end);

  List<ServiceInstance> findByServiceTemplateAndChurchInAndServiceDateBetween(
      ServiceTemplate template, Iterable<Church> churches, LocalDate start, LocalDate end);

  List<ServiceInstance> findByServiceTemplateAndServiceDateBetween(
      ServiceTemplate template, LocalDate start, LocalDate end);

  boolean existsByChurchAndServiceTemplateAndServiceDate(
      Church church, ServiceTemplate template, LocalDate serviceDate);
}
