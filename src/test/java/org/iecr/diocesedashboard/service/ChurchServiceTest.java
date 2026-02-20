package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.repositories.ChurchRepository;
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
class ChurchServiceTest {

    @Mock
    private ChurchRepository churchRepository;

    @InjectMocks
    private ChurchService churchService;

    @Test
    void findAll_returnsAllChurches() {
        Church c1 = new Church();
        Church c2 = new Church();
        when(churchRepository.findAll()).thenReturn(List.of(c1, c2));

        List<Church> result = churchService.findAll();

        assertThat(result).hasSize(2);
        verify(churchRepository).findAll();
    }

    @Test
    void findById_returnsPresent_whenExists() {
        Church church = new Church();
        when(churchRepository.findById("St. Mary")).thenReturn(Optional.of(church));

        Optional<Church> result = churchService.findById("St. Mary");

        assertThat(result).isPresent().contains(church);
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
        when(churchRepository.save(church)).thenReturn(church);

        Church result = churchService.save(church);

        assertThat(result).isEqualTo(church);
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
