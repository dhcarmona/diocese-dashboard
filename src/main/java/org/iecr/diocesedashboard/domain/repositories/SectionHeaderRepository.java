package org.iecr.diocesedashboard.domain.repositories;

import org.iecr.diocesedashboard.domain.objects.SectionHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data JPA repository for {@link SectionHeader} entities. */
public interface SectionHeaderRepository extends JpaRepository<SectionHeader, Long> {

  /**
   * Returns the highest sort_order value among section headers belonging to the given template,
   * or -1 if the template has no section headers yet.
   *
   * @param templateId the template ID
   * @return the maximum sortOrder, or -1
   */
  @Query("SELECT COALESCE(MAX(h.sortOrder), -1) FROM SectionHeader h"
      + " WHERE h.serviceTemplate.id = :templateId")
  int findMaxSortOrderByTemplateId(@Param("templateId") Long templateId);

  /**
   * Updates the sortOrder of a single SectionHeader in place.
   *
   * @param id        the section header ID
   * @param sortOrder the new sort order value
   */
  @Modifying
  @Query("UPDATE SectionHeader h SET h.sortOrder = :sortOrder WHERE h.id = :id")
  void updateSortOrder(@Param("id") Long id, @Param("sortOrder") int sortOrder);
}
