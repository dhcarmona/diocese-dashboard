package org.iecr.diocesedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemResponse;
import org.iecr.diocesedashboard.domain.repositories.ServiceInfoItemResponseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ServiceInfoItemResponseServiceTest {

  @Mock
  private ServiceInfoItemResponseRepository serviceInfoItemResponseRepository;

  @InjectMocks
  private ServiceInfoItemResponseService serviceInfoItemResponseService;

  @Test
  void findAll_returnsAllResponses() {
    ServiceInfoItemResponse r1 = new ServiceInfoItemResponse();
    ServiceInfoItemResponse r2 = new ServiceInfoItemResponse();
    when(serviceInfoItemResponseRepository.findAll()).thenReturn(List.of(r1, r2));

    List<ServiceInfoItemResponse> result = serviceInfoItemResponseService.findAll();

    assertThat(result).hasSize(2);
    verify(serviceInfoItemResponseRepository).findAll();
  }

  @Test
  void findById_returnsPresent_whenExists() {
    ServiceInfoItemResponse response = new ServiceInfoItemResponse();
    when(serviceInfoItemResponseRepository.findById(1L)).thenReturn(Optional.of(response));

    Optional<ServiceInfoItemResponse> result = serviceInfoItemResponseService.findById(1L);

    assertThat(result).isPresent().contains(response);
    verify(serviceInfoItemResponseRepository).findById(1L);
  }

  @Test
  void findById_returnsEmpty_whenNotExists() {
    when(serviceInfoItemResponseRepository.findById(99L)).thenReturn(Optional.empty());

    Optional<ServiceInfoItemResponse> result = serviceInfoItemResponseService.findById(99L);

    assertThat(result).isEmpty();
  }

  @Test
  void save_returnsSavedResponse() {
    ServiceInfoItemResponse response = new ServiceInfoItemResponse();
    when(serviceInfoItemResponseRepository.save(response)).thenReturn(response);

    ServiceInfoItemResponse result = serviceInfoItemResponseService.save(response);

    assertThat(result).isEqualTo(response);
    verify(serviceInfoItemResponseRepository).save(response);
  }

  @Test
  void deleteById_delegatesToRepository() {
    serviceInfoItemResponseService.deleteById(1L);

    verify(serviceInfoItemResponseRepository).deleteById(1L);
  }

  @Test
  void existsById_returnsTrue_whenExists() {
    when(serviceInfoItemResponseRepository.existsById(1L)).thenReturn(true);

    assertThat(serviceInfoItemResponseService.existsById(1L)).isTrue();
  }

  @Test
  void existsById_returnsFalse_whenNotExists() {
    when(serviceInfoItemResponseRepository.existsById(99L)).thenReturn(false);

    assertThat(serviceInfoItemResponseService.existsById(99L)).isFalse();
  }
}
