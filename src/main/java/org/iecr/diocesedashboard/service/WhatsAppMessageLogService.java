package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.WhatsAppMessageLog;
import org.iecr.diocesedashboard.domain.repositories.WhatsAppMessageLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Handles persistence of {@link WhatsAppMessageLog} entries.
 *
 * <p>Each save runs in its own transaction ({@code REQUIRES_NEW}) so that log entries
 * are written even when called from within a {@code afterCommit} callback, where the
 * original transaction has already been committed.
 */
@Service
public class WhatsAppMessageLogService {

  private final WhatsAppMessageLogRepository messageLogRepository;

  @Autowired
  public WhatsAppMessageLogService(WhatsAppMessageLogRepository messageLogRepository) {
    this.messageLogRepository = messageLogRepository;
  }

  /**
   * Persists a log entry for a regular WhatsApp message.
   *
   * @param recipientUsername dashboard username of the recipient
   * @param body              the message body to record
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void logMessage(String recipientUsername, String body) {
    save(recipientUsername, body, false);
  }

  /**
   * Persists a log entry for an OTP message, without storing the message content.
   *
   * @param recipientUsername dashboard username of the recipient
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void logOtp(String recipientUsername) {
    save(recipientUsername, null, true);
  }

  private void save(String recipientUsername, String body, boolean otp) {
    WhatsAppMessageLog log = new WhatsAppMessageLog();
    log.setSentAt(Instant.now());
    log.setRecipientUsername(recipientUsername);
    log.setBody(body);
    log.setOtp(otp);
    messageLogRepository.save(log);
  }
}
