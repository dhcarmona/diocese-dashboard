package org.iecr.diocesedashboard.domain.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.iecr.diocesedashboard.domain.objects.Celebrant;
import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
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

  @Test
  void findByServiceTemplateAndChurchAndServiceDateBetween_returnsOnlyInRange() {
    LocalDate jan1 = LocalDate.of(2024, 1, 1);
    LocalDate jan15 = LocalDate.of(2024, 1, 15);
    LocalDate feb1 = LocalDate.of(2024, 2, 1);

    ServiceInstance inRange = buildInstance();
    inRange.setServiceDate(jan15);
    ServiceInstance onBoundary = buildInstance();
    onBoundary.setServiceDate(feb1);
    ServiceInstance beforeRange = buildInstance();
    beforeRange.setServiceDate(LocalDate.of(2023, 12, 31));
    ServiceInstance afterRange = buildInstance();
    afterRange.setServiceDate(feb1.plusDays(1));

    entityManager.persist(inRange);
    entityManager.persist(onBoundary);
    entityManager.persist(beforeRange);
    entityManager.persist(afterRange);
    entityManager.flush();

    List<ServiceInstance> result =
        serviceInstanceRepository.findByServiceTemplateAndChurchAndServiceDateBetween(
            template, church, jan1, feb1);

    assertThat(result).hasSize(2);
    assertThat(result).extracting(ServiceInstance::getServiceDate)
        .containsExactlyInAnyOrder(jan15, feb1);
  }

  @Test
  void findByServiceTemplateAndChurchAndServiceDateBetween_excludesOtherChurch() {
    Church other = new Church();
    other.setName("Other");
    entityManager.persist(other);
    entityManager.flush();

    ServiceInstance mine = buildInstance();
    mine.setServiceDate(LocalDate.of(2024, 6, 1));
    ServiceInstance theirs = new ServiceInstance();
    theirs.setChurch(other);
    theirs.setServiceTemplate(template);
    theirs.setServiceDate(LocalDate.of(2024, 6, 1));

    entityManager.persist(mine);
    entityManager.persist(theirs);
    entityManager.flush();

    List<ServiceInstance> result =
        serviceInstanceRepository.findByServiceTemplateAndChurchAndServiceDateBetween(
            template, church, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getChurch().getName()).isEqualTo("St. Mary");
  }

  @Test
  void findByServiceTemplateAndServiceDateBetween_returnsAllChurches() {
    Church other = new Church();
    other.setName("Other");
    entityManager.persist(other);
    entityManager.flush();

    ServiceInstance first = buildInstance();
    first.setServiceDate(LocalDate.of(2024, 6, 1));
    ServiceInstance second = new ServiceInstance();
    second.setChurch(other);
    second.setServiceTemplate(template);
    second.setServiceDate(LocalDate.of(2024, 6, 1));

    entityManager.persist(first);
    entityManager.persist(second);
    entityManager.flush();

    List<ServiceInstance> result =
        serviceInstanceRepository.findByServiceTemplateAndServiceDateBetween(
            template, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

    assertThat(result).hasSize(2);
  }

  @Test
  void existsByChurchAndServiceTemplateAndServiceDate_returnsTrueWhenMatch() {
    LocalDate date = LocalDate.of(2024, 3, 10);
    ServiceInstance instance = buildInstance();
    instance.setServiceDate(date);
    entityManager.persistFlushFind(instance);

    assertThat(serviceInstanceRepository.existsByChurchAndServiceTemplateAndServiceDate(
        church, template, date)).isTrue();
  }

  @Test
  void celebrantsLinkedViaInstance_areVisibleOnLoad() {
    // Regression test: ServiceInstance must be the owning side of the ManyToMany so that
    // setCelebrants() actually writes to the join table.  If the owning side is flipped to
    // Celebrant (with @JoinTable there), this test fails — celebrants come back empty.
    Celebrant celebrant = new Celebrant();
    celebrant.setName("Rev. Alice");
    entityManager.persist(celebrant);

    ServiceInstance instance = buildInstance();
    instance.setCelebrants(Set.of(celebrant));
    entityManager.persist(instance);
    entityManager.flush();
    entityManager.clear();

    ServiceInstance loaded = entityManager.find(ServiceInstance.class, instance.getId());
    assertThat(loaded.getCelebrants()).isNotEmpty();
    assertThat(loaded.getCelebrants()).extracting(Celebrant::getName).containsExactly("Rev. Alice");
  }

  @Test
  void findByTemplateAndChurchAndDateRangeWithCelebrants_returnsCelebrantsEagerlyLoaded() {
    Celebrant celebrant = new Celebrant();
    celebrant.setName("Rev. Bob");
    entityManager.persist(celebrant);

    ServiceInstance instance = buildInstance();
    instance.setServiceDate(LocalDate.of(2024, 6, 15));
    instance.setCelebrants(Set.of(celebrant));
    entityManager.persist(instance);
    entityManager.flush();
    entityManager.clear();

    List<ServiceInstance> result =
        serviceInstanceRepository.findByTemplateAndChurchAndDateRangeWithCelebrants(
            template, church,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getCelebrants())
        .extracting(Celebrant::getName).containsExactly("Rev. Bob");
  }

  @Test
  void findByTemplateAndChurchAndDateRangeWithCelebrants_excludesOutOfRange() {
    ServiceInstance inRange = buildInstance();
    inRange.setServiceDate(LocalDate.of(2024, 6, 15));
    entityManager.persist(inRange);

    ServiceInstance outOfRange = buildInstance();
    outOfRange.setServiceDate(LocalDate.of(2023, 12, 31));
    entityManager.persist(outOfRange);
    entityManager.flush();
    entityManager.clear();

    List<ServiceInstance> result =
        serviceInstanceRepository.findByTemplateAndChurchAndDateRangeWithCelebrants(
            template, church,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getServiceDate()).isEqualTo(LocalDate.of(2024, 6, 15));
  }

  @Test
  void findByTemplateAndDateRangeWithCelebrants_coversAllChurches() {
    Church other = new Church();
    other.setName("Other Parish");
    entityManager.persist(other);

    Celebrant c1 = new Celebrant();
    c1.setName("Rev. Carol");
    entityManager.persist(c1);

    ServiceInstance first = buildInstance();
    first.setServiceDate(LocalDate.of(2024, 3, 1));
    first.setCelebrants(Set.of(c1));
    entityManager.persist(first);

    ServiceInstance second = new ServiceInstance();
    second.setChurch(other);
    second.setServiceTemplate(template);
    second.setServiceDate(LocalDate.of(2024, 3, 8));
    entityManager.persist(second);

    entityManager.flush();
    entityManager.clear();

    List<ServiceInstance> result =
        serviceInstanceRepository.findByTemplateAndDateRangeWithCelebrants(
            template,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

    assertThat(result).hasSize(2);
  }
}
