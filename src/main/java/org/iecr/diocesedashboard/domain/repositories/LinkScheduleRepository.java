package org.iecr.diocesedashboard.domain.repositories;

import org.iecr.diocesedashboard.domain.objects.LinkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link LinkSchedule} persistence. */
public interface LinkScheduleRepository extends JpaRepository<LinkSchedule, Long> {
}
