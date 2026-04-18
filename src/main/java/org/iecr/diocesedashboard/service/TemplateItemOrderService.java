package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.repositories.SectionHeaderRepository;
import org.iecr.diocesedashboard.domain.repositories.ServiceInfoItemRepository;
import org.iecr.diocesedashboard.webapp.controller.TemplateItemKind;
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
   * based on the order given in {@code items}. Runs in a single transaction.
   *
   * @param items the full ordered list of template item references
   */
  @Transactional
  public void reorder(List<TemplateItemRef> items) {
    for (int position = 0; position < items.size(); position++) {
      TemplateItemRef ref = items.get(position);
      if (ref.kind() == TemplateItemKind.INFO_ITEM) {
        infoItemRepository.updateSortOrder(ref.id(), position);
      } else {
        sectionHeaderRepository.updateSortOrder(ref.id(), position);
      }
    }
  }
}
