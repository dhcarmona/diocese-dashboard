package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.repositories.ServiceTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ServiceTemplateService {

  private final ServiceTemplateRepository repository;
  private final PortraitService portraitService;

  @Autowired
  public ServiceTemplateService(ServiceTemplateRepository repository,
      PortraitService portraitService) {
    this.repository = repository;
    this.portraitService = portraitService;
  }

  /**
   * Returns all service templates with their banner URLs attached.
   *
   * @return list of all service templates
   */
  public List<ServiceTemplate> findAll() {
    return repository.findAll().stream().map(this::attachBanner).toList();
  }

  /**
   * Returns service templates accessible to reporter users (link-only templates excluded).
   *
   * @return list of non-link-only service templates
   */
  public List<ServiceTemplate> findAllForReporter() {
    return repository.findByLinkOnlyFalse().stream().map(this::attachBanner).toList();
  }

  /**
   * Returns the service template with the given ID, with banner URL attached.
   *
   * @param id the template ID
   * @return optional containing the template, or empty if not found
   */
  public Optional<ServiceTemplate> findById(Long id) {
    return repository.findById(id).map(this::attachBanner);
  }

  /**
   * Saves the given service template and returns it with banner URL attached.
   *
   * @param serviceTemplate the template to save
   * @return the saved template with banner URL
   */
  public ServiceTemplate save(ServiceTemplate serviceTemplate) {
    return attachBanner(repository.save(serviceTemplate));
  }

  public void deleteById(Long id) {
    repository.deleteById(id);
  }

  public boolean existsById(Long id) {
    return repository.existsById(id);
  }

  private ServiceTemplate attachBanner(ServiceTemplate template) {
    template.setBannerUrl(
        portraitService.buildServiceTemplateBannerUrl(template.getServiceTemplateName()));
    return template;
  }
}
