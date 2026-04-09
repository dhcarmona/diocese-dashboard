package org.iecr.diocesedashboard.domain.repositories;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ServiceInstanceRepository extends JpaRepository<ServiceInstance, Long> {

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
