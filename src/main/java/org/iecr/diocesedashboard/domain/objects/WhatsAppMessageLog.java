package org.iecr.diocesedashboard.domain.objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.Instant;

/** A record of a WhatsApp message sent by the system. OTP message bodies are never stored. */
@Entity
@Table(name = "whatsapp_message_log")
public class WhatsAppMessageLog {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "whatsapp_message_log_seq")
  @SequenceGenerator(name = "whatsapp_message_log_seq", allocationSize = 50)
  private Long id;

  @Column(nullable = false)
  private Instant sentAt;

  @Column(nullable = false)
  private String recipientUsername;

  @Column(columnDefinition = "TEXT")
  private String body;

  @Column(nullable = false)
  private boolean otp;

  /** Default no-arg constructor required by JPA. */
  public WhatsAppMessageLog() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public void setSentAt(Instant sentAt) {
    this.sentAt = sentAt;
  }

  public String getRecipientUsername() {
    return recipientUsername;
  }

  public void setRecipientUsername(String recipientUsername) {
    this.recipientUsername = recipientUsername;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public boolean isOtp() {
    return otp;
  }

  public void setOtp(boolean otp) {
    this.otp = otp;
  }
}
