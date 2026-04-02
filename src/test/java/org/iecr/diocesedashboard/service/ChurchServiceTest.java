package org.iecr.diocesedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.repositories.ChurchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ChurchServiceTest {

  @Mock
  private ChurchRepository churchRepository;

  @Mock
  private PortraitService portraitService;

  @InjectMocks
  private ChurchService churchService;

  @Test
  void findAll_returnsAllChurches() {
    Church c1 = new Church();
    c1.setName("Cathedral");
    Church c2 = new Church();
    c2.setName("Trinity");
    when(churchRepository.findAll()).thenReturn(List.of(c1, c2));
    when(portraitService.buildChurchPortraitUrl("Cathedral")).thenReturn("portrait-cathedral");
    when(portraitService.buildChurchPortraitUrl("Trinity")).thenReturn("portrait-trinity");

    List<Church> result = churchService.findAll();

    assertThat(result).hasSize(2);
    assertThat(result).extracting(Church::getPortraitUrl)
        .containsExactly("portrait-cathedral", "portrait-trinity");
    verify(churchRepository).findAll();
  }

  @Test
  void findById_returnsPresent_whenExists() {
    Church church = new Church();
    church.setName("St. Mary");
    when(churchRepository.findById("St. Mary")).thenReturn(Optional.of(church));
    when(portraitService.buildChurchPortraitUrl("St. Mary")).thenReturn("portrait-mary");

    Optional<Church> result = churchService.findById("St. Mary");

    assertThat(result).isPresent().contains(church);
    assertThat(result.orElseThrow().getPortraitUrl()).isEqualTo("portrait-mary");
    verify(churchRepository).findById("St. Mary");
  }

  @Test
  void findById_returnsEmpty_whenNotExists() {
    when(churchRepository.findById("Unknown")).thenReturn(Optional.empty());

    Optional<Church> result = churchService.findById("Unknown");

    assertThat(result).isEmpty();
  }

  @Test
  void save_returnsSavedChurch() {
    Church church = new Church();
    church.setName("St. Luke");
    when(churchRepository.save(church)).thenReturn(church);
    when(portraitService.buildChurchPortraitUrl("St. Luke")).thenReturn("portrait-luke");

    Church result = churchService.save(church);

    assertThat(result).isEqualTo(church);
    assertThat(result.getPortraitUrl()).isEqualTo("portrait-luke");
    verify(churchRepository).save(church);
  }

  @Test
  void deleteById_delegatesToRepository() {
    churchService.deleteById("St. Mary");

    verify(churchRepository).deleteById("St. Mary");
  }

  @Test
  void existsById_returnsTrue_whenExists() {
    when(churchRepository.existsById("St. Mary")).thenReturn(true);

    assertThat(churchService.existsById("St. Mary")).isTrue();
  }

  @Test
  void existsById_returnsFalse_whenNotExists() {
    when(churchRepository.existsById("Unknown")).thenReturn(false);

    assertThat(churchService.existsById("Unknown")).isFalse();
  }
}
