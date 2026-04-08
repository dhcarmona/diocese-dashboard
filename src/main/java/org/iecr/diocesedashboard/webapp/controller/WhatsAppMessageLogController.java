package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.WhatsAppMessageLog;
import org.iecr.diocesedashboard.domain.repositories.WhatsAppMessageLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** REST controller exposing the WhatsApp message log to administrators. */
@RestController
@RequestMapping("/api/whatsapp-logs")
public class WhatsAppMessageLogController {

  private final WhatsAppMessageLogRepository messageLogRepository;

  @Autowired
  public WhatsAppMessageLogController(WhatsAppMessageLogRepository messageLogRepository) {
    this.messageLogRepository = messageLogRepository;
  }

  /**
   * Returns a paginated list of WhatsApp message log entries, newest first.
   *
   * @param pageable page/size/sort parameters; defaults to page 0, size 25, sorted by sentAt DESC
   * @return page of log entry responses
   */
  @GetMapping
  public Page<WhatsAppMessageLogResponse> getAll(
      @PageableDefault(size = 25, sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return messageLogRepository.findAll(pageable).map(WhatsAppMessageLogResponse::from);
  }

  /** Response DTO for a single WhatsApp message log entry. */
  public record WhatsAppMessageLogResponse(
  Long id,
  Instant sentAt,
  String recipientUsername,
  String body,
  boolean otp) {

    /** Creates a response from the given log entity. */
    public static WhatsAppMessageLogResponse from(WhatsAppMessageLog log) {
      return new WhatsAppMessageLogResponse(
          log.getId(),
          log.getSentAt(),
          log.getRecipientUsername(),
          log.getBody(),
          log.isOtp());
    }
  }
}
