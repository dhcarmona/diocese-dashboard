package org.iecr.diocesedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.repositories.ServiceTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ServiceTemplateServiceTest {

  @Mock
  private ServiceTemplateRepository repository;

  @Mock
  private PortraitService portraitService;

  @InjectMocks
  private ServiceTemplateService serviceTemplateService;

  private ServiceTemplate buildTemplate(String name) {
    ServiceTemplate t = new ServiceTemplate();
    t.setServiceTemplateName(name);
    return t;
  }

  @Test
  void findAll_returnsAllTemplatesWithBannerUrls() {
    ServiceTemplate t1 = buildTemplate("Sunday Mass");
    ServiceTemplate t2 = buildTemplate("Vespers");
    when(repository.findAll()).thenReturn(List.of(t1, t2));
    when(portraitService.buildServiceTemplateBannerUrl("Sunday Mass"))
        .thenReturn("/api/portraits/service-templates?name=Sunday+Mass");
    when(portraitService.buildServiceTemplateBannerUrl("Vespers"))
        .thenReturn("/api/portraits/service-templates?name=Vespers");

    List<ServiceTemplate> result = serviceTemplateService.findAll();

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getBannerUrl())
        .isEqualTo("/api/portraits/service-templates?name=Sunday+Mass");
    assertThat(result.get(1).getBannerUrl())
        .isEqualTo("/api/portraits/service-templates?name=Vespers");
    verify(repository).findAll();
  }

  @Test
  void findById_returnsTemplateWithBannerUrl_whenExists() {
    ServiceTemplate template = buildTemplate("Sunday Mass");
    when(repository.findById(1L)).thenReturn(Optional.of(template));
    when(portraitService.buildServiceTemplateBannerUrl("Sunday Mass"))
        .thenReturn("/api/portraits/service-templates?name=Sunday+Mass");

    Optional<ServiceTemplate> result = serviceTemplateService.findById(1L);

    assertThat(result).isPresent();
    assertThat(result.get().getBannerUrl())
        .isEqualTo("/api/portraits/service-templates?name=Sunday+Mass");
    verify(repository).findById(1L);
  }

  @Test
  void findById_returnsEmpty_whenNotExists() {
    when(repository.findById(99L)).thenReturn(Optional.empty());

    Optional<ServiceTemplate> result = serviceTemplateService.findById(99L);

    assertThat(result).isEmpty();
  }

  @Test
  void save_returnsSavedTemplateWithBannerUrl() {
    ServiceTemplate template = buildTemplate("Sunday Mass");
    when(repository.save(template)).thenReturn(template);
    when(portraitService.buildServiceTemplateBannerUrl("Sunday Mass"))
        .thenReturn("/api/portraits/service-templates?name=Sunday+Mass");

    ServiceTemplate result = serviceTemplateService.save(template);

    assertThat(result).isEqualTo(template);
    assertThat(result.getBannerUrl())
        .isEqualTo("/api/portraits/service-templates?name=Sunday+Mass");
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
