package org.iecr.diocesedashboard.domain.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.iecr.diocesedashboard.domain.objects.Celebrant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

@DataJpaTest
class CelebrantRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private CelebrantRepository celebrantRepository;

  private Celebrant buildCelebrant(String name) {
    Celebrant celebrant = new Celebrant();
    celebrant.setName(name);
    return celebrant;
  }

  @Test
  void save_persistsAndAssignsId() {
    Celebrant saved = celebrantRepository.save(buildCelebrant("Father John"));

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getName()).isEqualTo("Father John");
  }

  @Test
  void findById_returnsPresent_whenExists() {
    Celebrant celebrant = entityManager.persistFlushFind(buildCelebrant("Father John"));

    Optional<Celebrant> result = celebrantRepository.findById(celebrant.getId());

    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("Father John");
  }

  @Test
  void findById_returnsEmpty_whenNotExists() {
    Optional<Celebrant> result = celebrantRepository.findById(999L);

    assertThat(result).isEmpty();
  }

  @Test
  void findAll_returnsAllPersistedCelebrants() {
    entityManager.persist(buildCelebrant("Father John"));
    entityManager.persist(buildCelebrant("Father Paul"));
    entityManager.flush();

    List<Celebrant> result = celebrantRepository.findAll();

    assertThat(result).hasSize(2);
  }

  @Test
  void deleteById_removesEntity() {
    Celebrant celebrant = entityManager.persistFlushFind(buildCelebrant("Father John"));

    celebrantRepository.deleteById(celebrant.getId());
    entityManager.flush();

    assertThat(celebrantRepository.findById(celebrant.getId())).isEmpty();
  }

  @Test
  void existsById_returnsTrue_whenExists() {
    Celebrant celebrant = entityManager.persistFlushFind(buildCelebrant("Father John"));

    assertThat(celebrantRepository.existsById(celebrant.getId())).isTrue();
  }

  @Test
  void existsById_returnsFalse_whenNotExists() {
    assertThat(celebrantRepository.existsById(999L)).isFalse();
  }
}
