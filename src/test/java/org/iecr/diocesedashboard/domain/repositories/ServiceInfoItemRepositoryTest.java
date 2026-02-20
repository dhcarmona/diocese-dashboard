package org.iecr.diocesedashboard.domain.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemType;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

@DataJpaTest
class ServiceInfoItemRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private ServiceInfoItemRepository serviceInfoItemRepository;

  private ServiceTemplate template;

  @BeforeEach
  void setUp() {
    ServiceTemplate t = new ServiceTemplate();
    t.setServiceTemplateName("Sunday Mass");
    template = entityManager.persistAndFlush(t);
  }

  private ServiceInfoItem buildItem(String questionId) {
    ServiceInfoItem item = new ServiceInfoItem();
    item.setQuestionId(questionId);
    item.setServiceTemplate(template);
    item.setRequired(true);
    item.setServiceInfoItemType(ServiceInfoItemType.STRING);
    return item;
  }

  @Test
  void save_persistsAndAssignsId() {
    ServiceInfoItem saved = serviceInfoItemRepository.save(buildItem("q1"));

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getQuestionId()).isEqualTo("q1");
  }

  @Test
  void findById_returnsPresent_whenExists() {
    ServiceInfoItem item = entityManager.persistFlushFind(buildItem("q1"));

    Optional<ServiceInfoItem> result = serviceInfoItemRepository.findById(item.getId());

    assertThat(result).isPresent();
    assertThat(result.get().getQuestionId()).isEqualTo("q1");
  }

  @Test
  void findById_returnsEmpty_whenNotExists() {
    Optional<ServiceInfoItem> result = serviceInfoItemRepository.findById(999L);

    assertThat(result).isEmpty();
  }

  @Test
  void findAll_returnsAllPersistedItems() {
    entityManager.persist(buildItem("q1"));
    entityManager.persist(buildItem("q2"));
    entityManager.flush();

    List<ServiceInfoItem> result = serviceInfoItemRepository.findAll();

    assertThat(result).hasSize(2);
  }

  @Test
  void deleteById_removesEntity() {
    ServiceInfoItem item = entityManager.persistFlushFind(buildItem("q1"));

    serviceInfoItemRepository.deleteById(item.getId());
    entityManager.flush();

    assertThat(serviceInfoItemRepository.findById(item.getId())).isEmpty();
  }

  @Test
  void existsById_returnsTrue_whenExists() {
    ServiceInfoItem item = entityManager.persistFlushFind(buildItem("q1"));

    assertThat(serviceInfoItemRepository.existsById(item.getId())).isTrue();
  }

  @Test
  void existsById_returnsFalse_whenNotExists() {
    assertThat(serviceInfoItemRepository.existsById(999L)).isFalse();
  }
}
