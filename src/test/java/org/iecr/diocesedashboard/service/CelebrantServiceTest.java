package org.iecr.diocesedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.iecr.diocesedashboard.domain.objects.Celebrant;
import org.iecr.diocesedashboard.domain.repositories.CelebrantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CelebrantServiceTest {

  @Mock
  private CelebrantRepository celebrantRepository;

  @InjectMocks
  private CelebrantService celebrantService;

  @Test
  void findAll_returnsAllCelebrants() {
    Celebrant c1 = new Celebrant();
    Celebrant c2 = new Celebrant();
    when(celebrantRepository.findAll()).thenReturn(List.of(c1, c2));

    List<Celebrant> result = celebrantService.findAll();

    assertThat(result).hasSize(2);
    verify(celebrantRepository).findAll();
  }

  @Test
  void findById_returnsPresent_whenExists() {
    Celebrant celebrant = new Celebrant();
    when(celebrantRepository.findById(1L)).thenReturn(Optional.of(celebrant));

    Optional<Celebrant> result = celebrantService.findById(1L);

    assertThat(result).isPresent().contains(celebrant);
    verify(celebrantRepository).findById(1L);
  }

  @Test
  void findById_returnsEmpty_whenNotExists() {
    when(celebrantRepository.findById(99L)).thenReturn(Optional.empty());

    Optional<Celebrant> result = celebrantService.findById(99L);

    assertThat(result).isEmpty();
  }

  @Test
  void save_returnsSavedCelebrant() {
    Celebrant celebrant = new Celebrant();
    when(celebrantRepository.save(celebrant)).thenReturn(celebrant);

    Celebrant result = celebrantService.save(celebrant);

    assertThat(result).isEqualTo(celebrant);
    verify(celebrantRepository).save(celebrant);
  }

  @Test
  void deleteById_delegatesToRepository() {
    celebrantService.deleteById(1L);

    verify(celebrantRepository).deleteById(1L);
  }

  @Test
  void existsById_returnsTrue_whenExists() {
    when(celebrantRepository.existsById(1L)).thenReturn(true);

    assertThat(celebrantService.existsById(1L)).isTrue();
  }

  @Test
  void existsById_returnsFalse_whenNotExists() {
    when(celebrantRepository.existsById(99L)).thenReturn(false);

    assertThat(celebrantService.existsById(99L)).isFalse();
  }
}
