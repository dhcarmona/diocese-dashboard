package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.repositories.ServiceInstanceRepository;
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
class ServiceInstanceServiceTest {

    @Mock
    private ServiceInstanceRepository repository;

    @InjectMocks
    private ServiceInstanceService serviceInstanceService;

    @Test
    void findAll_returnsAllInstances() {
        ServiceInstance i1 = new ServiceInstance();
        ServiceInstance i2 = new ServiceInstance();
        when(repository.findAll()).thenReturn(List.of(i1, i2));

        List<ServiceInstance> result = serviceInstanceService.findAll();

        assertThat(result).hasSize(2);
        verify(repository).findAll();
    }

    @Test
    void findById_returnsInstance_whenExists() {
        ServiceInstance instance = new ServiceInstance();
        when(repository.findById(1L)).thenReturn(Optional.of(instance));

        Optional<ServiceInstance> result = serviceInstanceService.findById(1L);

        assertThat(result).isPresent().contains(instance);
        verify(repository).findById(1L);
    }

    @Test
    void findById_returnsEmpty_whenNotExists() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Optional<ServiceInstance> result = serviceInstanceService.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void save_returnsSavedInstance() {
        ServiceInstance instance = new ServiceInstance();
        when(repository.save(instance)).thenReturn(instance);

        ServiceInstance result = serviceInstanceService.save(instance);

        assertThat(result).isEqualTo(instance);
        verify(repository).save(instance);
    }

    @Test
    void deleteById_delegatesToRepository() {
        serviceInstanceService.deleteById(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void existsById_returnsTrue_whenExists() {
        when(repository.existsById(1L)).thenReturn(true);

        assertThat(serviceInstanceService.existsById(1L)).isTrue();
    }

    @Test
    void existsById_returnsFalse_whenNotExists() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThat(serviceInstanceService.existsById(99L)).isFalse();
    }
}
