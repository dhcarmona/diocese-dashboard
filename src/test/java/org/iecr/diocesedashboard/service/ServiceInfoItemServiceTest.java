package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.repositories.ServiceInfoItemRepository;
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
class ServiceInfoItemServiceTest {

    @Mock
    private ServiceInfoItemRepository serviceInfoItemRepository;

    @InjectMocks
    private ServiceInfoItemService serviceInfoItemService;

    @Test
    void findAll_returnsAllItems() {
        ServiceInfoItem i1 = new ServiceInfoItem();
        ServiceInfoItem i2 = new ServiceInfoItem();
        when(serviceInfoItemRepository.findAll()).thenReturn(List.of(i1, i2));

        List<ServiceInfoItem> result = serviceInfoItemService.findAll();

        assertThat(result).hasSize(2);
        verify(serviceInfoItemRepository).findAll();
    }

    @Test
    void findById_returnsPresent_whenExists() {
        ServiceInfoItem item = new ServiceInfoItem();
        when(serviceInfoItemRepository.findById(1L)).thenReturn(Optional.of(item));

        Optional<ServiceInfoItem> result = serviceInfoItemService.findById(1L);

        assertThat(result).isPresent().contains(item);
        verify(serviceInfoItemRepository).findById(1L);
    }

    @Test
    void findById_returnsEmpty_whenNotExists() {
        when(serviceInfoItemRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<ServiceInfoItem> result = serviceInfoItemService.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void save_returnsSavedItem() {
        ServiceInfoItem item = new ServiceInfoItem();
        when(serviceInfoItemRepository.save(item)).thenReturn(item);

        ServiceInfoItem result = serviceInfoItemService.save(item);

        assertThat(result).isEqualTo(item);
        verify(serviceInfoItemRepository).save(item);
    }

    @Test
    void deleteById_delegatesToRepository() {
        serviceInfoItemService.deleteById(1L);

        verify(serviceInfoItemRepository).deleteById(1L);
    }

    @Test
    void existsById_returnsTrue_whenExists() {
        when(serviceInfoItemRepository.existsById(1L)).thenReturn(true);

        assertThat(serviceInfoItemService.existsById(1L)).isTrue();
    }

    @Test
    void existsById_returnsFalse_whenNotExists() {
        when(serviceInfoItemRepository.existsById(99L)).thenReturn(false);

        assertThat(serviceInfoItemService.existsById(99L)).isFalse();
    }
}
