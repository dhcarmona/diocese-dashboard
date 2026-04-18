package org.iecr.diocesedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.iecr.diocesedashboard.domain.objects.SectionHeader;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.repositories.SectionHeaderRepository;
import org.iecr.diocesedashboard.domain.repositories.ServiceInfoItemRepository;
import org.iecr.diocesedashboard.webapp.controller.TemplateItemKind;
import org.iecr.diocesedashboard.webapp.controller.TemplateItemRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class TemplateItemOrderServiceTest {

  @Mock
  private ServiceInfoItemRepository infoItemRepository;

  @Mock
  private SectionHeaderRepository sectionHeaderRepository;

  @InjectMocks
  private TemplateItemOrderService templateItemOrderService;

  // --- getNextSortOrder ---

  @Test
  void getNextSortOrder_returnsMaxAcrossBothTables() {
    when(infoItemRepository.findMaxSortOrderByTemplateId(1L)).thenReturn(5);
    when(sectionHeaderRepository.findMaxSortOrderByTemplateId(1L)).thenReturn(3);

    int result = templateItemOrderService.getNextSortOrder(1L);

    assertThat(result).isEqualTo(6);
  }

  @Test
  void getNextSortOrder_usesHeaderMax_whenHeaderMaxIsHigher() {
    when(infoItemRepository.findMaxSortOrderByTemplateId(1L)).thenReturn(2);
    when(sectionHeaderRepository.findMaxSortOrderByTemplateId(1L)).thenReturn(10);

    int result = templateItemOrderService.getNextSortOrder(1L);

    assertThat(result).isEqualTo(11);
  }

  @Test
  void getNextSortOrder_returnsOne_whenBothTablesEmpty() {
    when(infoItemRepository.findMaxSortOrderByTemplateId(1L)).thenReturn(0);
    when(sectionHeaderRepository.findMaxSortOrderByTemplateId(1L)).thenReturn(0);

    int result = templateItemOrderService.getNextSortOrder(1L);

    assertThat(result).isEqualTo(1);
  }

  // --- reorder ---

  @Test
  void reorder_updatesInfoItemSortOrder() {
    Long templateId = 1L;
    ServiceTemplate template = new ServiceTemplate();
    template.setId(templateId);

    ServiceInfoItem item = new ServiceInfoItem();
    item.setId(10L);
    item.setServiceTemplate(template);

    when(infoItemRepository.findById(10L)).thenReturn(Optional.of(item));

    templateItemOrderService.reorder(templateId, List.of(
        new TemplateItemRef(10L, TemplateItemKind.INFO_ITEM)
    ));

    verify(infoItemRepository).updateSortOrder(10L, 0);
  }

  @Test
  void reorder_updatesSectionHeaderSortOrder() {
    Long templateId = 1L;
    ServiceTemplate template = new ServiceTemplate();
    template.setId(templateId);

    SectionHeader header = new SectionHeader();
    header.setId(20L);
    header.setServiceTemplate(template);

    when(sectionHeaderRepository.findById(20L)).thenReturn(Optional.of(header));

    templateItemOrderService.reorder(templateId, List.of(
        new TemplateItemRef(20L, TemplateItemKind.SECTION_HEADER)
    ));

    verify(sectionHeaderRepository).updateSortOrder(20L, 0);
  }

  @Test
  void reorder_assignsPositionsByIndex() {
    Long templateId = 1L;
    ServiceTemplate template = new ServiceTemplate();
    template.setId(templateId);

    ServiceInfoItem item1 = new ServiceInfoItem();
    item1.setId(1L);
    item1.setServiceTemplate(template);

    SectionHeader header = new SectionHeader();
    header.setId(2L);
    header.setServiceTemplate(template);

    ServiceInfoItem item2 = new ServiceInfoItem();
    item2.setId(3L);
    item2.setServiceTemplate(template);

    when(infoItemRepository.findById(1L)).thenReturn(Optional.of(item1));
    when(sectionHeaderRepository.findById(2L)).thenReturn(Optional.of(header));
    when(infoItemRepository.findById(3L)).thenReturn(Optional.of(item2));

    templateItemOrderService.reorder(templateId, List.of(
        new TemplateItemRef(1L, TemplateItemKind.INFO_ITEM),
        new TemplateItemRef(2L, TemplateItemKind.SECTION_HEADER),
        new TemplateItemRef(3L, TemplateItemKind.INFO_ITEM)
    ));

    verify(infoItemRepository).updateSortOrder(1L, 0);
    verify(sectionHeaderRepository).updateSortOrder(2L, 1);
    verify(infoItemRepository).updateSortOrder(3L, 2);
  }

  @Test
  void reorder_throwsIllegalArgument_whenInfoItemDoesNotBelongToTemplate() {
    Long templateId = 1L;
    ServiceTemplate otherTemplate = new ServiceTemplate();
    otherTemplate.setId(99L);

    ServiceInfoItem item = new ServiceInfoItem();
    item.setId(10L);
    item.setServiceTemplate(otherTemplate);

    when(infoItemRepository.findById(10L)).thenReturn(Optional.of(item));

    assertThatThrownBy(() ->
        templateItemOrderService.reorder(templateId, List.of(
            new TemplateItemRef(10L, TemplateItemKind.INFO_ITEM)
        ))
    ).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not belong to template");
  }

  @Test
  void reorder_throwsIllegalArgument_whenSectionHeaderDoesNotBelongToTemplate() {
    Long templateId = 1L;
    ServiceTemplate otherTemplate = new ServiceTemplate();
    otherTemplate.setId(99L);

    SectionHeader header = new SectionHeader();
    header.setId(20L);
    header.setServiceTemplate(otherTemplate);

    when(sectionHeaderRepository.findById(20L)).thenReturn(Optional.of(header));

    assertThatThrownBy(() ->
        templateItemOrderService.reorder(templateId, List.of(
            new TemplateItemRef(20L, TemplateItemKind.SECTION_HEADER)
        ))
    ).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not belong to template");
  }

  @Test
  void reorder_throwsIllegalArgument_whenInfoItemNotFound() {
    when(infoItemRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() ->
        templateItemOrderService.reorder(1L, List.of(
            new TemplateItemRef(99L, TemplateItemKind.INFO_ITEM)
        ))
    ).isInstanceOf(IllegalArgumentException.class);
  }
}
