package org.iecr.diocesedashboard.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

/**
 * Service for sending WhatsApp messages via the Twilio API.
 *
 * <p>The following environment variables must be configured:
 * <ul>
 *   <li>{@code TWILIO_ACCOUNT_SID} — Twilio account SID</li>
 *   <li>{@code TWILIO_AUTH_TOKEN} — Twilio auth token</li>
 *   <li>{@code TWILIO_WHATSAPP_FROM} — Sender number in E.164 format (e.g. {@code +50600000000});
 *       defaults to the Twilio sandbox number {@code +14155238886}</li>
 * </ul>
 *
 * <p>When using the sandbox, each recipient must first opt in by sending
 * {@code join &lt;sandbox-keyword&gt;} to the sandbox number on WhatsApp.
 * Switching to a production number only requires changing {@code TWILIO_WHATSAPP_FROM}.
 */
@Service
public class WhatsAppService {

  private static final Logger LOG = Logger.getLogger(WhatsAppService.class.getName());
  private static final String WHATSAPP_PREFIX = "whatsapp:";

  private final String accountSid;
  private final String authToken;
  private final String fromNumber;
  private final WhatsAppMessageLogService messageLogService;

  @Autowired
  public WhatsAppService(
      @Value("${twilio.account-sid}") String accountSid,
      @Value("${twilio.auth-token}") String authToken,
      @Value("${twilio.whatsapp.from}") String fromNumber,
      WhatsAppMessageLogService messageLogService) {
    this.accountSid = accountSid;
    this.authToken = authToken;
    this.fromNumber = fromNumber;
    this.messageLogService = messageLogService;
  }

  /** Initializes the Twilio client with the configured credentials. */
  @PostConstruct
  void initialize() {
    Twilio.init(accountSid, authToken);
  }

  /**
   * Sends a WhatsApp message to the given phone number.
   *
   * @param to   recipient phone number in E.164 format (e.g. {@code +50688888888})
   * @param body the message text
   */
  public void sendMessage(String to, String body) {
    dispatchMessage(WHATSAPP_PREFIX + fromNumber, WHATSAPP_PREFIX + to, body);
  }

  /**
   * Sends a WhatsApp message and records a summary in the message log.
   * Use this when the message body contains sensitive or verbose data (e.g. URLs with tokens)
   * that should not be stored verbatim.
   *
   * @param to                recipient phone number in E.164 format
   * @param body              the full message text to send
   * @param logSummary        the redacted summary to store in the log
   * @param recipientUsername the dashboard username of the recipient
   */
  public void sendMessageAndLog(String to, String body, String logSummary,
      String recipientUsername) {
    sendMessage(to, body);
    tryLogMessage(recipientUsername, logSummary);
  }

  /**
   * Sends a WhatsApp message and records it in the message log.
   *
   * @param to                recipient phone number in E.164 format
   * @param body              the message text
   * @param recipientUsername the dashboard username of the recipient
   */
  public void sendMessageAndLog(String to, String body, String recipientUsername) {
    sendMessage(to, body);
    tryLogMessage(recipientUsername, body);
  }

  /**
   * Sends an OTP WhatsApp message and records it in the message log without storing the content.
   *
   * @param to                recipient phone number in E.164 format
   * @param body              the OTP message text (not persisted)
   * @param recipientUsername the dashboard username of the recipient
   */
  public void sendOtpAndLog(String to, String body, String recipientUsername) {
    sendMessage(to, body);
    tryLogOtp(recipientUsername);
  }

  private void tryLogMessage(String recipientUsername, String body) {
    try {
      messageLogService.logMessage(recipientUsername, body);
    } catch (Exception ex) {
      LOG.warning("Failed to log WhatsApp message for "
          + recipientUsername + ": " + ex.getMessage());
    }
  }

  private void tryLogOtp(String recipientUsername) {
    try {
      messageLogService.logOtp(recipientUsername);
    } catch (Exception ex) {
      LOG.warning("Failed to log WhatsApp OTP for " + recipientUsername + ": " + ex.getMessage());
    }
  }

  /** Dispatches the message via Twilio; package-private to allow spy-based testing. */
  void dispatchMessage(String from, String to, String body) {
    Message.creator(new PhoneNumber(to), new PhoneNumber(from), body).create();
  }
}
