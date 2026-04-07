package org.iecr.diocesedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.repositories.ServiceInfoItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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

  @Test
  void createItem_assignsNextSortOrderAndSaves() {
    ServiceTemplate template = new ServiceTemplate();
    template.setId(1L);
    ServiceInfoItem item = new ServiceInfoItem();
    item.setServiceTemplate(template);
    when(serviceInfoItemRepository.findMaxSortOrderByTemplateId(1L)).thenReturn(2);
    when(serviceInfoItemRepository.save(item)).thenReturn(item);

    ServiceInfoItem result = serviceInfoItemService.createItem(item);

    assertThat(result.getSortOrder()).isEqualTo(3);
    verify(serviceInfoItemRepository).save(item);
  }

  @Test
  void createItem_assignsZeroSortOrderWhenTemplateHasNoItems() {
    ServiceTemplate template = new ServiceTemplate();
    template.setId(2L);
    ServiceInfoItem item = new ServiceInfoItem();
    item.setServiceTemplate(template);
    when(serviceInfoItemRepository.findMaxSortOrderByTemplateId(2L)).thenReturn(-1);
    when(serviceInfoItemRepository.save(item)).thenReturn(item);

    ServiceInfoItem result = serviceInfoItemService.createItem(item);

    assertThat(result.getSortOrder()).isEqualTo(0);
    verify(serviceInfoItemRepository).save(item);
  }

  @Test
  void reorder_updatesEachItemWithItsPosition() {
    List<Long> orderedIds = List.of(3L, 1L, 2L);

    serviceInfoItemService.reorder(orderedIds);

    verify(serviceInfoItemRepository).updateSortOrder(3L, 0);
    verify(serviceInfoItemRepository).updateSortOrder(1L, 1);
    verify(serviceInfoItemRepository).updateSortOrder(2L, 2);
  }
}
