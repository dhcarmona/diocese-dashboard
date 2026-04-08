package org.iecr.diocesedashboard.domain.repositories;

import org.iecr.diocesedashboard.domain.objects.WhatsAppMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link WhatsAppMessageLog} persistence. */
public interface WhatsAppMessageLogRepository extends JpaRepository<WhatsAppMessageLog, Long> {
}
