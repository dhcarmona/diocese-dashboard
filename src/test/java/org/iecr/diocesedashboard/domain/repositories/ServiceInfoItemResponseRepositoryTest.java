package org.iecr.diocesedashboard.domain.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemResponse;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemType;
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
class ServiceInfoItemResponseRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private ServiceInfoItemResponseRepository serviceInfoItemResponseRepository;

  private ServiceInfoItem serviceInfoItem;
  private ServiceInstance serviceInstance;

  @BeforeEach
  void setUp() {
    Church church = new Church();
    church.setName("St. Mary");
    church.setLocation("Downtown");
    entityManager.persist(church);

    ServiceTemplate template = new ServiceTemplate();
    template.setServiceTemplateName("Sunday Mass");
    entityManager.persist(template);

    ServiceInfoItem item = new ServiceInfoItem();
    item.setTitle("q1");
    item.setServiceTemplate(template);
    item.setRequired(true);
    item.setServiceInfoItemType(ServiceInfoItemType.STRING);
    serviceInfoItem = entityManager.persist(item);

    ServiceInstance instance = new ServiceInstance();
    instance.setChurch(church);
    instance.setServiceTemplate(template);
    serviceInstance = entityManager.persist(instance);

    entityManager.flush();
  }

  private ServiceInfoItemResponse buildResponse() {
    ServiceInfoItemResponse response = new ServiceInfoItemResponse();
    response.setServiceInfoItem(serviceInfoItem);
    response.setServiceInstance(serviceInstance);
    return response;
  }

  @Test
  void save_persistsAndAssignsId() {
    ServiceInfoItemResponse saved = serviceInfoItemResponseRepository.save(buildResponse());

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getServiceInfoItem().getTitle()).isEqualTo("q1");
  }

  @Test
  void findById_returnsPresent_whenExists() {
    ServiceInfoItemResponse response = entityManager.persistFlushFind(buildResponse());

    Optional<ServiceInfoItemResponse> result =
        serviceInfoItemResponseRepository.findById(response.getId());

    assertThat(result).isPresent();
  }

  @Test
  void findById_returnsEmpty_whenNotExists() {
    Optional<ServiceInfoItemResponse> result =
        serviceInfoItemResponseRepository.findById(999L);

    assertThat(result).isEmpty();
  }

  @Test
  void findAll_returnsAllPersistedResponses() {
    entityManager.persist(buildResponse());
    entityManager.persist(buildResponse());
    entityManager.flush();

    List<ServiceInfoItemResponse> result = serviceInfoItemResponseRepository.findAll();

    assertThat(result).hasSize(2);
  }

  @Test
  void findByServiceInstanceInWithItems_returnsResponsesWithItemsLoaded() {
    ServiceInfoItemResponse response = buildResponse();
    response.setResponseValue("42");
    entityManager.persist(response);
    entityManager.flush();
    entityManager.clear();

    List<ServiceInfoItemResponse> result =
        serviceInfoItemResponseRepository.findByServiceInstanceInWithItems(
            List.of(serviceInstance));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getResponseValue()).isEqualTo("42");
    assertThat(result.get(0).getServiceInfoItem()).isNotNull();
    assertThat(result.get(0).getServiceInfoItem().getTitle()).isEqualTo("q1");
  }

  @Test
  void findByServiceInstanceInWithItems_returnsEmptyForUnrelatedInstance() {
    ServiceInstance other = new ServiceInstance();
    other.setChurch(entityManager.find(
        Church.class, "St. Mary"));
    other.setServiceTemplate(entityManager.find(
        ServiceTemplate.class,
        serviceInstance.getServiceTemplate().getId()));
    entityManager.persist(other);

    entityManager.persist(buildResponse());
    entityManager.flush();
    entityManager.clear();

    List<ServiceInfoItemResponse> result =
        serviceInfoItemResponseRepository.findByServiceInstanceInWithItems(List.of(other));

    assertThat(result).isEmpty();
  }

  @Test
  void findByServiceInstancesAndItem_returnsMatchingResponse() {
    ServiceInfoItemResponse response = buildResponse();
    response.setResponseValue("99");
    entityManager.persist(response);
    entityManager.flush();
    entityManager.clear();

    List<ServiceInfoItemResponse> result =
        serviceInfoItemResponseRepository.findByServiceInstancesAndItem(
            List.of(serviceInstance), serviceInfoItem);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getResponseValue()).isEqualTo("99");
    assertThat(result.get(0).getServiceInfoItem().getTitle()).isEqualTo("q1");
    assertThat(result.get(0).getServiceInstance().getId()).isEqualTo(serviceInstance.getId());
  }

  @Test
  void findByServiceInstancesAndItem_excludesResponsesForDifferentItem() {
    ServiceTemplate template = entityManager.find(
        ServiceTemplate.class, serviceInfoItem.getServiceTemplate().getId());

    ServiceInfoItem otherItem = new ServiceInfoItem();
    otherItem.setTitle("q2");
    otherItem.setServiceTemplate(template);
    otherItem.setRequired(false);
    otherItem.setServiceInfoItemType(ServiceInfoItemType.NUMERICAL);
    ServiceInfoItem persistedOther = entityManager.persist(otherItem);

    ServiceInfoItemResponse r1 = buildResponse();
    r1.setResponseValue("10");
    entityManager.persist(r1);

    ServiceInfoItemResponse r2 = new ServiceInfoItemResponse();
    r2.setServiceInfoItem(persistedOther);
    r2.setServiceInstance(serviceInstance);
    r2.setResponseValue("20");
    entityManager.persist(r2);

    entityManager.flush();
    entityManager.clear();

    List<ServiceInfoItemResponse> result =
        serviceInfoItemResponseRepository.findByServiceInstancesAndItem(
            List.of(serviceInstance), serviceInfoItem);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getResponseValue()).isEqualTo("10");
  }

  @Test
  void findByServiceInstancesAndItem_excludesResponsesForDifferentInstance() {
    ServiceInstance otherInstance = new ServiceInstance();
    otherInstance.setChurch(entityManager.find(Church.class, "St. Mary"));
    otherInstance.setServiceTemplate(serviceInstance.getServiceTemplate());
    entityManager.persist(otherInstance);

    ServiceInfoItemResponse r1 = buildResponse();
    r1.setResponseValue("50");
    entityManager.persist(r1);

    ServiceInfoItemResponse r2 = new ServiceInfoItemResponse();
    r2.setServiceInfoItem(serviceInfoItem);
    r2.setServiceInstance(otherInstance);
    r2.setResponseValue("100");
    entityManager.persist(r2);

    entityManager.flush();
    entityManager.clear();

    List<ServiceInfoItemResponse> result =
        serviceInfoItemResponseRepository.findByServiceInstancesAndItem(
            List.of(serviceInstance), serviceInfoItem);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getResponseValue()).isEqualTo("50");
  }

  @Test
  void deleteById_removesEntity() {
    ServiceInfoItemResponse response = entityManager.persistFlushFind(buildResponse());

    serviceInfoItemResponseRepository.deleteById(response.getId());
    entityManager.flush();

    assertThat(serviceInfoItemResponseRepository.findById(response.getId())).isEmpty();
  }

  @Test
  void existsById_returnsTrue_whenExists() {
    ServiceInfoItemResponse response = entityManager.persistFlushFind(buildResponse());

    assertThat(serviceInfoItemResponseRepository.existsById(response.getId())).isTrue();
  }

  @Test
  void existsById_returnsFalse_whenNotExists() {
    assertThat(serviceInfoItemResponseRepository.existsById(999L)).isFalse();
  }
}
