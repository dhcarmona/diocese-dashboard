package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.Celebrant;
import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemResponse;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.webapp.controller.ServiceInstanceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

  public ServiceInstance submit(Long templateId, ServiceInstanceRequest request) {
    ServiceTemplate template = serviceTemplateService.findById(templateId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));

    Church church = churchService.findById(request.churchName())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Church not found"));

    ServiceInstance instance = new ServiceInstance();
    instance.setServiceTemplate(template);
    instance.setChurch(church);
    instance.setServiceDate(request.serviceDate());

    if (request.celebrantIds() != null && !request.celebrantIds().isEmpty()) {
      Set<Celebrant> celebrants = new HashSet<>();
      for (Long celebrantId : request.celebrantIds()) {
        Celebrant celebrant = celebrantService.findById(celebrantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Celebrant not found: " + celebrantId));
        celebrants.add(celebrant);
      }
      instance.setCelebrants(celebrants);
    }

    ServiceInstance saved = serviceInstanceService.save(instance);

    if (request.responses() != null) {
      for (ServiceInstanceRequest.ResponseEntry entry : request.responses()) {
        ServiceInfoItem item = serviceInfoItemService.findById(entry.serviceInfoItemId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "ServiceInfoItem not found: " + entry.serviceInfoItemId()));
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
