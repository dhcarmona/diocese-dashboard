package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages one-time passcodes (OTPs) for reporter user authentication.
 *
 * <p>OTPs are 6-digit codes valid for {@value #OTP_TTL_SECONDS} seconds.
 * They are stored in memory and consumed on first successful verification.
 */
@Service
public class ReporterOtpService {

  static final long OTP_TTL_SECONDS = 600;
  private static final int OTP_DIGITS = 6;
  private static final int OTP_MODULUS = 1_000_000;

  private static final Locale WHATSAPP_LOCALE = Locale.forLanguageTag("es");

  private final WhatsAppService whatsAppService;
  private final UserService userService;
  private final MessageSource messageSource;
  private final SecureRandom secureRandom = new SecureRandom();
  private final ConcurrentHashMap<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

  @Autowired
  public ReporterOtpService(
      WhatsAppService whatsAppService, UserService userService, MessageSource messageSource) {
    this.whatsAppService = whatsAppService;
    this.userService = userService;
    this.messageSource = messageSource;
  }

  /**
   * Generates an OTP for the given reporter username and sends it via WhatsApp.
   * Any previously stored (possibly expired) OTP for this username is replaced.
   * Expired entries for other users are evicted opportunistically on each call.
   *
   * @param username the reporter's username
   * @throws IllegalArgumentException if no enabled reporter with that username exists,
   *                                  or if the reporter has no phone number registered
   */
  public void generateAndSendOtp(String username) {
    DashboardUser user = userService.findByUsername(username)
        .filter(DashboardUser::isEnabled)
        .filter(uu -> uu.getRole() == UserRole.REPORTER)
        .orElseThrow(() -> new IllegalArgumentException(
            "No active reporter found for username: " + username));

    String phoneNumber = user.getPhoneNumber();
    if (phoneNumber == null || phoneNumber.isBlank()) {
      throw new IllegalArgumentException(
          "Reporter does not have a valid phone number for username: " + username);
    }

    evictExpiredEntries();

    String code = generateCode();
    otpStore.put(username, new OtpEntry(code, Instant.now().plusSeconds(OTP_TTL_SECONDS)));
    whatsAppService.sendMessage(phoneNumber,
        messageSource.getMessage("otp.whatsapp.message", new Object[]{code}, WHATSAPP_LOCALE));
  }

  /**
   * Verifies the OTP for the given username and consumes it if valid.
   *
   * @param username the reporter's username
   * @param code     the OTP provided by the user
   * @return {@code true} if the code is correct and not expired; {@code false} otherwise
   */
  public boolean verifyAndConsumeOtp(String username, String code) {
    OtpEntry entry = otpStore.get(username);
    if (entry == null) {
      return false;
    }
    if (Instant.now().isAfter(entry.expiry())) {
      otpStore.remove(username);
      return false;
    }
    if (!entry.code().equals(code)) {
      return false;
    }
    otpStore.remove(username);
    return true;
  }

  private void evictExpiredEntries() {
    Instant now = Instant.now();
    otpStore.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiry()));
  }

  private String generateCode() {
    int code = secureRandom.nextInt(OTP_MODULUS);
    return String.format("%0" + OTP_DIGITS + "d", code);
  }

  /** Immutable OTP entry holding the code and its expiry instant. */
  record OtpEntry(String code, Instant expiry) {
  }
}
