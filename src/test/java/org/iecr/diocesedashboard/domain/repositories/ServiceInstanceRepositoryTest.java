package org.iecr.diocesedashboard.domain.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

@DataJpaTest
class ServiceInstanceRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private ServiceInstanceRepository serviceInstanceRepository;

  private Church church;
  private ServiceTemplate template;

  @BeforeEach
  void setUp() {
    Church c = new Church();
    c.setName("St. Mary");
    c.setLocation("Downtown");
    church = entityManager.persist(c);

    ServiceTemplate t = new ServiceTemplate();
    t.setServiceTemplateName("Sunday Mass");
    template = entityManager.persist(t);

    entityManager.flush();
  }

  private ServiceInstance buildInstance() {
    ServiceInstance instance = new ServiceInstance();
    instance.setChurch(church);
    instance.setServiceTemplate(template);
    return instance;
  }

  @Test
  void save_persistsAndAssignsId() {
    ServiceInstance saved = serviceInstanceRepository.save(buildInstance());

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getChurch().getName()).isEqualTo("St. Mary");
    assertThat(saved.getServiceTemplate().getServiceTemplateName()).isEqualTo("Sunday Mass");
  }

  @Test
  void findById_returnsPresent_whenExists() {
    ServiceInstance instance = entityManager.persistFlushFind(buildInstance());

    Optional<ServiceInstance> result = serviceInstanceRepository.findById(instance.getId());

    assertThat(result).isPresent();
  }

  @Test
  void findById_returnsEmpty_whenNotExists() {
    Optional<ServiceInstance> result = serviceInstanceRepository.findById(999L);

    assertThat(result).isEmpty();
  }

  @Test
  void findAll_returnsAllPersistedInstances() {
    entityManager.persist(buildInstance());
    entityManager.persist(buildInstance());
    entityManager.flush();

    List<ServiceInstance> result = serviceInstanceRepository.findAll();

    assertThat(result).hasSize(2);
  }

  @Test
  void deleteById_removesEntity() {
    ServiceInstance instance = entityManager.persistFlushFind(buildInstance());

    serviceInstanceRepository.deleteById(instance.getId());
    entityManager.flush();

    assertThat(serviceInstanceRepository.findById(instance.getId())).isEmpty();
  }

  @Test
  void existsById_returnsTrue_whenExists() {
    ServiceInstance instance = entityManager.persistFlushFind(buildInstance());

    assertThat(serviceInstanceRepository.existsById(instance.getId())).isTrue();
  }

  @Test
  void existsById_returnsFalse_whenNotExists() {
    assertThat(serviceInstanceRepository.existsById(999L)).isFalse();
  }

  @Test
  void findByChurch_returnsOnlyMatchingInstances() {
    Church otherChurch = new Church();
    otherChurch.setName("St. Joseph");
    otherChurch.setLocation("Uptown");
    entityManager.persist(otherChurch);
    entityManager.flush();

    ServiceInstance forStMary = buildInstance();
    ServiceInstance forStJoseph = new ServiceInstance();
    forStJoseph.setChurch(otherChurch);
    forStJoseph.setServiceTemplate(template);

    entityManager.persist(forStMary);
    entityManager.persist(forStJoseph);
    entityManager.flush();

    List<ServiceInstance> result = serviceInstanceRepository.findByChurch(church);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getChurch().getName()).isEqualTo("St. Mary");
  }
}
