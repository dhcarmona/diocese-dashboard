package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.repositories.ServiceTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceTemplateServiceTest {

    @Mock
    private ServiceTemplateRepository repository;

    @InjectMocks
    private ServiceTemplateService serviceTemplateService;

    @Test
    void findAll_returnsAllTemplates() {
        ServiceTemplate t1 = new ServiceTemplate();
        ServiceTemplate t2 = new ServiceTemplate();
        when(repository.findAll()).thenReturn(List.of(t1, t2));

        List<ServiceTemplate> result = serviceTemplateService.findAll();

        assertThat(result).hasSize(2);
        verify(repository).findAll();
    }

    @Test
    void findById_returnsPresent_whenExists() {
        ServiceTemplate template = new ServiceTemplate();
        when(repository.findById(1L)).thenReturn(Optional.of(template));

        Optional<ServiceTemplate> result = serviceTemplateService.findById(1L);

        assertThat(result).isPresent().contains(template);
        verify(repository).findById(1L);
    }

    @Test
    void findById_returnsEmpty_whenNotExists() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Optional<ServiceTemplate> result = serviceTemplateService.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void save_returnsSavedTemplate() {
        ServiceTemplate template = new ServiceTemplate();
        when(repository.save(template)).thenReturn(template);

        ServiceTemplate result = serviceTemplateService.save(template);

        assertThat(result).isEqualTo(template);
        verify(repository).save(template);
    }

    @Test
    void deleteById_delegatesToRepository() {
        serviceTemplateService.deleteById(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void existsById_returnsTrue_whenExists() {
        when(repository.existsById(1L)).thenReturn(true);

        assertThat(serviceTemplateService.existsById(1L)).isTrue();
    }

    @Test
    void existsById_returnsFalse_whenNotExists() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThat(serviceTemplateService.existsById(99L)).isFalse();
    }
}
