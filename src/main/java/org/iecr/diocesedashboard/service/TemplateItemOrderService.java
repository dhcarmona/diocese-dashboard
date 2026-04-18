package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.repositories.SectionHeaderRepository;
import org.iecr.diocesedashboard.domain.repositories.ServiceInfoItemRepository;
import org.iecr.diocesedashboard.webapp.controller.TemplateItemRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service that manages sort order across both {@code ServiceInfoItem} and {@code SectionHeader}
 * entities within a template, treating them as a single unified ordered list.
 */
@Service
public class TemplateItemOrderService {

  private final ServiceInfoItemRepository infoItemRepository;
  private final SectionHeaderRepository sectionHeaderRepository;

  @Autowired
  public TemplateItemOrderService(ServiceInfoItemRepository infoItemRepository,
      SectionHeaderRepository sectionHeaderRepository) {
    this.infoItemRepository = infoItemRepository;
    this.sectionHeaderRepository = sectionHeaderRepository;
  }

  /**
   * Returns the next sort order position to use when appending a new item to a template.
   * Considers both info items and section headers so all items share one sortOrder namespace.
   *
   * @param templateId the template ID
   * @return the next available sortOrder value
   */
  public int getNextSortOrder(Long templateId) {
    int maxInfoItem = infoItemRepository.findMaxSortOrderByTemplateId(templateId);
    int maxHeader = sectionHeaderRepository.findMaxSortOrderByTemplateId(templateId);
    return Math.max(maxInfoItem, maxHeader) + 1;
  }

  /**
   * Assigns sequential sortOrder values to all template items (info items and section headers)
   * based on the order given in {@code items}. Each item is validated to belong to the given
   * template before its sort order is updated. Runs in a single transaction.
   *
   * @param templateId the template ID that owns all referenced items
   * @param items      the full ordered list of template item references
   * @throws IllegalArgumentException if an item does not belong to the specified template
   *                                  or an unknown kind is encountered
   */
  @Transactional
  public void reorder(Long templateId, List<TemplateItemRef> items) {
    for (int position = 0; position < items.size(); position++) {
      TemplateItemRef ref = items.get(position);
      switch (ref.kind()) {
        case INFO_ITEM -> {
          validateInfoItemBelongsToTemplate(ref.id(), templateId);
          infoItemRepository.updateSortOrder(ref.id(), position);
        }
        case SECTION_HEADER -> {
          validateSectionHeaderBelongsToTemplate(ref.id(), templateId);
          sectionHeaderRepository.updateSortOrder(ref.id(), position);
        }
        default -> throw new IllegalArgumentException(
            "Unsupported template item kind: " + ref.kind());
      }
    }
  }

  private void validateInfoItemBelongsToTemplate(Long itemId, Long templateId) {
    boolean belongs = infoItemRepository.findById(itemId)
        .map(item -> item.getServiceTemplate() != null
            && templateId.equals(item.getServiceTemplate().getId()))
        .orElse(false);
    if (!belongs) {
      throw new IllegalArgumentException(
          "Info item " + itemId + " does not belong to template " + templateId);
    }
  }

  private void validateSectionHeaderBelongsToTemplate(Long headerId, Long templateId) {
    boolean belongs = sectionHeaderRepository.findById(headerId)
        .map(header -> header.getServiceTemplate() != null
            && templateId.equals(header.getServiceTemplate().getId()))
        .orElse(false);
    if (!belongs) {
      throw new IllegalArgumentException(
          "Section header " + headerId + " does not belong to template " + templateId);
    }
  }
}
