package org.iecr.diocesedashboard.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

  private static final String WHATSAPP_PREFIX = "whatsapp:";

  private final String accountSid;
  private final String authToken;
  private final String fromNumber;

  @Autowired
  public WhatsAppService(
      @Value("${twilio.account-sid}") String accountSid,
      @Value("${twilio.auth-token}") String authToken,
      @Value("${twilio.whatsapp.from}") String fromNumber) {
    this.accountSid = accountSid;
    this.authToken = authToken;
    this.fromNumber = fromNumber;
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

  /** Dispatches the message via Twilio; package-private to allow spy-based testing. */
  void dispatchMessage(String from, String to, String body) {
    Message.creator(new PhoneNumber(to), new PhoneNumber(from), body).create();
  }
}
