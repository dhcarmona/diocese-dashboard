package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.Celebrant;
import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemResponse;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.webapp.controller.ServiceInstanceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Orchestrates the creation of a ServiceInstance from a ServiceTemplate submission,
 * including persisting all associated ServiceInfoItemResponses.
 */
@Service
public class ServiceSubmissionService {

  private final ServiceTemplateService serviceTemplateService;
  private final ServiceInstanceService serviceInstanceService;
  private final ChurchService churchService;
  private final CelebrantService celebrantService;
  private final ServiceInfoItemService serviceInfoItemService;
  private final ServiceInfoItemResponseService responseService;

  @Autowired
  public ServiceSubmissionService(ServiceTemplateService serviceTemplateService,
      ServiceInstanceService serviceInstanceService,
      ChurchService churchService,
      CelebrantService celebrantService,
      ServiceInfoItemService serviceInfoItemService,
      ServiceInfoItemResponseService responseService) {
    this.serviceTemplateService = serviceTemplateService;
    this.serviceInstanceService = serviceInstanceService;
    this.churchService = churchService;
    this.celebrantService = celebrantService;
    this.serviceInfoItemService = serviceInfoItemService;
    this.responseService = responseService;
  }

  /**
   * Creates and persists a new {@link ServiceInstance} from the given template and request,
   * including all associated celebrants and survey responses.
   *
   * @param templateId  the ID of the {@link ServiceTemplate} to use
   * @param request     the submission request containing church, celebrants, date, and responses
   * @param submittedBy the {@link DashboardUser} who is submitting the report (may be null)
   * @return the saved {@link ServiceInstance}
   */
  @Transactional
  public ServiceInstance submit(Long templateId, ServiceInstanceRequest request,
      DashboardUser submittedBy) {
    ServiceTemplate template = serviceTemplateService.findById(templateId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));

    Church church = churchService.findById(request.churchName())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Church not found"));

    ServiceInstance instance = new ServiceInstance();
    instance.setServiceTemplate(template);
    instance.setChurch(church);
    instance.setServiceDate(request.serviceDate());
    instance.setSubmittedBy(submittedBy);
    instance.setSubmittedAt(LocalDateTime.now());

    if (request.celebrantIds() != null && !request.celebrantIds().isEmpty()) {
      Set<Celebrant> celebrants = new HashSet<>();
      for (Long celebrantId : request.celebrantIds()) {
        Celebrant celebrant = celebrantService.findById(celebrantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Celebrant not found: " + celebrantId));
        celebrants.add(celebrant);
      }
      instance.setCelebrants(celebrants);
    }

    ServiceInstance saved = serviceInstanceService.save(instance);

    if (request.responses() != null) {
      for (ServiceInstanceRequest.ResponseEntry entry : request.responses()) {
        ServiceInfoItem item = serviceInfoItemService.findById(entry.serviceInfoItemId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "ServiceInfoItem not found: " + entry.serviceInfoItemId()));
        if (item.getServiceTemplate() == null
            || !item.getServiceTemplate().getId().equals(template.getId())) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "ServiceInfoItem " + entry.serviceInfoItemId()
                  + " does not belong to template " + template.getId());
        }
        ServiceInfoItemResponse response = new ServiceInfoItemResponse();
        response.setServiceInfoItem(item);
        response.setServiceInstance(saved);
        response.setResponseValue(entry.responseValue());
        responseService.save(response);
      }
    }

    return saved;
  }
}
