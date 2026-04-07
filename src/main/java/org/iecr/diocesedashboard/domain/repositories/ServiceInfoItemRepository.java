package org.iecr.diocesedashboard.domain.repositories;

import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceInfoItemRepository extends JpaRepository<ServiceInfoItem, Long> {

  /**
   * Returns the highest sort_order value among items belonging to the given template,
   * or -1 if the template has no items yet.
   *
   * @param templateId the template ID
   * @return the maximum sortOrder, or -1
   */
  @Query("SELECT COALESCE(MAX(s.sortOrder), -1) FROM ServiceInfoItem s"
      + " WHERE s.serviceTemplate.id = :templateId")
  int findMaxSortOrderByTemplateId(@Param("templateId") Long templateId);

  /**
   * Updates the sortOrder of a single ServiceInfoItem in place.
   *
   * @param id        the item ID
   * @param sortOrder the new sort order value
   */
  @Modifying
  @Query("UPDATE ServiceInfoItem s SET s.sortOrder = :sortOrder WHERE s.id = :id")
  void updateSortOrder(@Param("id") Long id, @Param("sortOrder") int sortOrder);
}
