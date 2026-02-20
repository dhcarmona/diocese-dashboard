package org.iecr.diocesedashboard.domain.repositories;

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

import static org.assertj.core.api.Assertions.assertThat;

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
        ServiceTemplate template = new ServiceTemplate();
        template.setServiceTemplateName("Sunday Mass");
        entityManager.persist(template);

        ServiceInfoItem item = new ServiceInfoItem();
        item.setQuestionId("q1");
        item.setServiceTemplate(template);
        item.setRequired(true);
        item.setServiceInfoItemType(ServiceInfoItemType.STRING);
        serviceInfoItem = entityManager.persist(item);

        Church church = new Church();
        church.setName("St. Mary");
        church.setLocation("Downtown");
        entityManager.persist(church);

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
        assertThat(saved.getServiceInfoItem().getQuestionId()).isEqualTo("q1");
    }

    @Test
    void findById_returnsPresent_whenExists() {
        ServiceInfoItemResponse response = entityManager.persistFlushFind(buildResponse());

        Optional<ServiceInfoItemResponse> result = serviceInfoItemResponseRepository.findById(response.getId());

        assertThat(result).isPresent();
    }

    @Test
    void findById_returnsEmpty_whenNotExists() {
        Optional<ServiceInfoItemResponse> result = serviceInfoItemResponseRepository.findById(999L);

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
