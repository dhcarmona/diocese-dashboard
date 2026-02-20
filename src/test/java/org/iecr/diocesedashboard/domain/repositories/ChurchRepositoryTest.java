package org.iecr.diocesedashboard.domain.repositories;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ChurchRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChurchRepository churchRepository;

    private Church buildChurch(String name, String location) {
        Church church = new Church();
        church.setName(name);
        church.setLocation(location);
        return church;
    }

    @Test
    void save_persistsChurch() {
        Church saved = churchRepository.save(buildChurch("St. Mary", "Downtown"));

        assertThat(saved.getName()).isEqualTo("St. Mary");
        assertThat(saved.getLocation()).isEqualTo("Downtown");
    }

    @Test
    void findById_returnsPresent_whenExists() {
        entityManager.persistAndFlush(buildChurch("St. Mary", "Downtown"));

        Optional<Church> result = churchRepository.findById("St. Mary");

        assertThat(result).isPresent();
        assertThat(result.get().getLocation()).isEqualTo("Downtown");
    }

    @Test
    void findById_returnsEmpty_whenNotExists() {
        Optional<Church> result = churchRepository.findById("Unknown Church");

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_returnsAllPersistedChurches() {
        entityManager.persist(buildChurch("St. Mary", "Downtown"));
        entityManager.persist(buildChurch("St. Joseph", "Uptown"));
        entityManager.flush();

        List<Church> result = churchRepository.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void deleteById_removesEntity() {
        entityManager.persistAndFlush(buildChurch("St. Mary", "Downtown"));

        churchRepository.deleteById("St. Mary");
        entityManager.flush();

        assertThat(churchRepository.findById("St. Mary")).isEmpty();
    }

    @Test
    void existsById_returnsTrue_whenExists() {
        entityManager.persistAndFlush(buildChurch("St. Mary", "Downtown"));

        assertThat(churchRepository.existsById("St. Mary")).isTrue();
    }

    @Test
    void existsById_returnsFalse_whenNotExists() {
        assertThat(churchRepository.existsById("Unknown Church")).isFalse();
    }
}
