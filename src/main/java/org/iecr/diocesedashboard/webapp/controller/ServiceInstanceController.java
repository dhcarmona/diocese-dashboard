package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.Celebrant;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemResponse;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.service.CelebrantService;
import org.iecr.diocesedashboard.service.ServiceInfoItemResponseService;
import org.iecr.diocesedashboard.service.ServiceInfoItemService;
import org.iecr.diocesedashboard.service.ServiceInstanceService;
import org.iecr.diocesedashboard.service.ServiceTemplateService;
import org.iecr.diocesedashboard.service.WhatsAppService;
import org.iecr.diocesedashboard.webapp.DashboardUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** REST controller for reading, updating and deleting service instances. */
@RestController
@RequestMapping("/api/service-instances")
public class ServiceInstanceController {

  private final ServiceInstanceService serviceInstanceService;
  private final ServiceInfoItemResponseService responseService;
  private final ServiceInfoItemService serviceInfoItemService;
  private final ServiceTemplateService serviceTemplateService;
  private final CelebrantService celebrantService;
  private final WhatsAppService whatsAppService;
  private final MessageSource messageSource;

  @Autowired
  public ServiceInstanceController(ServiceInstanceService serviceInstanceService,
      ServiceInfoItemResponseService responseService,
      ServiceInfoItemService serviceInfoItemService,
      ServiceTemplateService serviceTemplateService,
      CelebrantService celebrantService,
      WhatsAppService whatsAppService,
      MessageSource messageSource) {
    this.serviceInstanceService = serviceInstanceService;
    this.responseService = responseService;
    this.serviceInfoItemService = serviceInfoItemService;
    this.serviceTemplateService = serviceTemplateService;
    this.celebrantService = celebrantService;
    this.whatsAppService = whatsAppService;
    this.messageSource = messageSource;
  }

  /**
   * Returns service instances visible to the authenticated user.
   * When {@code templateId} is provided (ADMIN only), filters by template.
   * REPORTER users only see instances for their assigned churches.
   *
   * @param templateId optional template filter (ADMIN only)
   * @param auth       the current authentication
   * @return list of summary responses
   */
  @GetMapping
  public List<ServiceInstanceSummaryResponse> getAll(
      @RequestParam(required = false) Long templateId,
      Authentication auth) {
    DashboardUser user = ((DashboardUserDetails) auth.getPrincipal()).getDashboardUser();
    List<ServiceInstance> instances;
    if (templateId != null) {
      if (user.getRole() != UserRole.ADMIN) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Template filter is only available to ADMIN users");
      }
      ServiceTemplate template = serviceTemplateService.findById(templateId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
              "Template not found"));
      instances = serviceInstanceService.findByServiceTemplate(template);
    } else if (user.getRole() == UserRole.REPORTER) {
      if (user.getAssignedChurches().isEmpty()) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Reporter account has no churches assigned");
      }
      instances = serviceInstanceService.findByChurches(user.getAssignedChurches());
    } else {
      instances = serviceInstanceService.findAll();
    }
    return instances.stream()
        .map(i -> ServiceInstanceSummaryResponse.from(i, user.getRole() == UserRole.ADMIN))
        .toList();
  }

  /**
   * Returns the full detail of a service instance, including all responses.
   * REPORTER users receive 404 if the instance does not belong to their church.
   *
   * @param id   the service instance ID
   * @param auth the current authentication
   * @return 200 with the detail, or 404 if not found or not accessible
   */
  @GetMapping("/{id}")
  public ResponseEntity<ServiceInstanceDetailResponse> getById(
      @PathVariable Long id, Authentication auth) {
    DashboardUser user = ((DashboardUserDetails) auth.getPrincipal()).getDashboardUser();
    Optional<ServiceInstance> found = serviceInstanceService.findByIdWithCelebrants(id)
        .filter(inst -> isAccessible(inst, user));
    if (found.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    ServiceInstance instance = found.get();
    List<ServiceInfoItemResponse> responses = responseService.findByServiceInstance(instance);
    return ResponseEntity.ok(ServiceInstanceDetailResponse.from(instance, responses));
  }

  /**
   * Updates the responses of a service instance and optionally notifies the reporter via WhatsApp.
   * ADMIN only.
   *
   * @param id      the service instance ID
   * @param request updated responses and notification flag
   * @return 200 with the updated detail, or 404 if not found
   */
  @PutMapping("/{id}")
  public ResponseEntity<ServiceInstanceDetailResponse> update(
      @PathVariable Long id,
      @RequestBody ServiceInstanceUpdateRequest request) {
    ServiceInstance instance = serviceInstanceService.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Service instance not found"));

    List<ServiceInfoItemResponse> existing = responseService.findByServiceInstance(instance);
    Map<Long, ServiceInfoItemResponse> byItemId = existing.stream()
        .collect(Collectors.toMap(
            r -> r.getServiceInfoItem().getId(), Function.identity(),
            (first, second) -> second));

    List<String> changeSummaries = new ArrayList<>();

    if (request.responses() != null) {
      for (ServiceInstanceUpdateRequest.ResponseEntry entry : request.responses()) {
        ServiceInfoItemResponse response = byItemId.get(entry.serviceInfoItemId());
        String oldValue = response != null ? response.getResponseValue() : null;
        String newValue = entry.responseValue();
        boolean changed = !Objects.equals(oldValue, newValue);
        if (changed) {
          ServiceInfoItem item = response != null
              ? response.getServiceInfoItem()
              : serviceInfoItemService.findById(entry.serviceInfoItemId())
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                  "Unknown service info item: " + entry.serviceInfoItemId()));
          if (response == null) {
            ServiceTemplate instanceTemplate = instance.getServiceTemplate();
            ServiceTemplate itemTemplate = item.getServiceTemplate();
            if (instanceTemplate != null && itemTemplate != null
                && !instanceTemplate.getId().equals(itemTemplate.getId())) {
              throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                  "Service info item " + item.getId()
                      + " does not belong to this report's template");
            }
          }
          String title = item.getTitle();
          changeSummaries.add("\"" + title + "\": \"" + oldValue + "\" → \"" + newValue + "\"");
          if (response != null) {
            response.setResponseValue(newValue);
            responseService.save(response);
          } else {
            ServiceInfoItemResponse newResponse = new ServiceInfoItemResponse();
            newResponse.setServiceInstance(instance);
            newResponse.setServiceInfoItem(item);
            newResponse.setResponseValue(newValue);
            responseService.save(newResponse);
          }
        }
      }
    }

    if (request.celebrantIds() != null) {
      List<Celebrant> resolved = celebrantService.findAllById(request.celebrantIds());
      if (resolved.size() != request.celebrantIds().size()) {
        Set<Long> resolvedIds = resolved.stream()
            .map(Celebrant::getId).collect(Collectors.toSet());
        List<Long> missing = request.celebrantIds().stream()
            .filter(celebrantId -> !resolvedIds.contains(celebrantId))
            .toList();
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Unknown celebrant IDs: " + missing);
      }
      Set<Long> oldIds = instance.getCelebrants() == null ? Set.of()
          : instance.getCelebrants().stream().map(Celebrant::getId).collect(Collectors.toSet());
      Set<Long> newIds = resolved.stream().map(Celebrant::getId).collect(Collectors.toSet());
      if (!oldIds.equals(newIds)) {
        String oldNames = instance.getCelebrants() == null ? ""
            : instance.getCelebrants().stream()
            .map(Celebrant::getName).sorted().collect(Collectors.joining(", "));
        String newNames = resolved.stream()
            .map(Celebrant::getName).sorted().collect(Collectors.joining(", "));
        changeSummaries.add("\"Celebrants\": \"" + oldNames + "\" → \"" + newNames + "\"");
      }
      instance.setCelebrants(new HashSet<>(resolved));
      serviceInstanceService.save(instance);
    }

    if (request.notifyReporter() && !changeSummaries.isEmpty()) {
      try {
        sendEditNotification(instance, changeSummaries);
      } catch (Exception ex) {
        // Notification failure is non-fatal; data has already been saved
      }
    }

    List<ServiceInfoItemResponse> updated = responseService.findByServiceInstance(instance);
    return ResponseEntity.ok(ServiceInstanceDetailResponse.from(instance, updated));
  }

  /**
   * Deletes the service instance with the given ID and optionally notifies the reporter.
   * ADMIN only.
   *
   * @param id     the service instance ID
   * @param notify if true and the instance has a reporter with a phone number, sends a WhatsApp
   *               message
   * @return 204 on success, or 404 if not found
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
      @PathVariable Long id,
      @RequestParam(required = false, defaultValue = "false") boolean notify) {
    ServiceInstance instance = serviceInstanceService.findById(id)
        .orElse(null);
    if (instance == null) {
      return ResponseEntity.notFound().build();
    }
    if (notify) {
      try {
        sendDeleteNotification(instance);
      } catch (Exception ex) {
        // Notification failure is non-fatal; deletion will still proceed
      }
    }
    responseService.deleteByServiceInstance(instance);
    serviceInstanceService.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  private void sendEditNotification(ServiceInstance instance, List<String> changes) {
    DashboardUser reporter = instance.getSubmittedBy();
    if (reporter == null || reporter.getPhoneNumber() == null
        || reporter.getPhoneNumber().isBlank()) {
      return;
    }
    String templateName = instance.getServiceTemplate() != null
        ? instance.getServiceTemplate().getServiceTemplateName() : "";
    String church = instance.getChurch() != null ? instance.getChurch().getName() : "";
    String date = instance.getServiceDate() != null
        ? instance.getServiceDate().toString() : "";
    String changeList = String.join("\n", changes);
    String body = messageSource.getMessage(
        "whatsapp.report.updated",
        new Object[]{templateName, church, date, changeList},
        reporter.getPreferredLocale());
    whatsAppService.sendMessageAndLog(reporter.getPhoneNumber(), body, reporter.getUsername());
  }

  private void sendDeleteNotification(ServiceInstance instance) {
    DashboardUser reporter = instance.getSubmittedBy();
    if (reporter == null || reporter.getPhoneNumber() == null
        || reporter.getPhoneNumber().isBlank()) {
      return;
    }
    String templateName = instance.getServiceTemplate() != null
        ? instance.getServiceTemplate().getServiceTemplateName() : "";
    String church = instance.getChurch() != null ? instance.getChurch().getName() : "";
    String date = instance.getServiceDate() != null
        ? instance.getServiceDate().toString() : "";
    String body = messageSource.getMessage(
        "whatsapp.report.deleted",
        new Object[]{templateName, church, date},
        reporter.getPreferredLocale());
    whatsAppService.sendMessageAndLog(reporter.getPhoneNumber(), body, reporter.getUsername());
  }

  private boolean isAccessible(ServiceInstance instance, DashboardUser user) {
    if (user.getRole() == UserRole.ADMIN) {
      return true;
    }
    return user.isAssignedToChurch(instance.getChurch());
  }
}
