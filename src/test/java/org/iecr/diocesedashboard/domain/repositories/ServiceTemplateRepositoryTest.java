package org.iecr.diocesedashboard.domain.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

@DataJpaTest
class ServiceTemplateRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private ServiceTemplateRepository serviceTemplateRepository;

  private ServiceTemplate buildTemplate(String name) {
    ServiceTemplate template = new ServiceTemplate();
    template.setServiceTemplateName(name);
    return template;
  }

  @Test
  void save_persistsAndAssignsId() {
    ServiceTemplate saved = serviceTemplateRepository.save(buildTemplate("Sunday Mass"));

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getServiceTemplateName()).isEqualTo("Sunday Mass");
  }

  @Test
  void findById_returnsPresent_whenExists() {
    ServiceTemplate template = entityManager.persistFlushFind(buildTemplate("Sunday Mass"));

    Optional<ServiceTemplate> result = serviceTemplateRepository.findById(template.getId());

    assertThat(result).isPresent();
    assertThat(result.get().getServiceTemplateName()).isEqualTo("Sunday Mass");
  }

  @Test
  void findById_returnsEmpty_whenNotExists() {
    Optional<ServiceTemplate> result = serviceTemplateRepository.findById(999L);

    assertThat(result).isEmpty();
  }

  @Test
  void findAll_returnsAllPersistedTemplates() {
    entityManager.persist(buildTemplate("Sunday Mass"));
    entityManager.persist(buildTemplate("Baptism"));
    entityManager.flush();

    List<ServiceTemplate> result = serviceTemplateRepository.findAll();

    assertThat(result).hasSize(2);
  }

  @Test
  void deleteById_removesEntity() {
    ServiceTemplate template = entityManager.persistFlushFind(buildTemplate("Sunday Mass"));

    serviceTemplateRepository.deleteById(template.getId());
    entityManager.flush();

    assertThat(serviceTemplateRepository.findById(template.getId())).isEmpty();
  }

  @Test
  void existsById_returnsTrue_whenExists() {
    ServiceTemplate template = entityManager.persistFlushFind(buildTemplate("Sunday Mass"));

    assertThat(serviceTemplateRepository.existsById(template.getId())).isTrue();
  }

  @Test
  void existsById_returnsFalse_whenNotExists() {
    assertThat(serviceTemplateRepository.existsById(999L)).isFalse();
  }
}
