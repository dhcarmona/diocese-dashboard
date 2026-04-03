package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
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

  private final WhatsAppService whatsAppService;
  private final UserService userService;
  private final SecureRandom secureRandom = new SecureRandom();
  private final ConcurrentHashMap<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

  @Autowired
  public ReporterOtpService(WhatsAppService whatsAppService, UserService userService) {
    this.whatsAppService = whatsAppService;
    this.userService = userService;
  }

  /**
   * Generates an OTP for the given reporter username and sends it via WhatsApp.
   *
   * @param username the reporter's username
   * @throws IllegalArgumentException if no enabled reporter with that username exists
   */
  public void generateAndSendOtp(String username) {
    DashboardUser user = userService.findByUsername(username)
        .filter(DashboardUser::isEnabled)
        .filter(uu -> uu.getRole() == UserRole.REPORTER)
        .orElseThrow(() -> new IllegalArgumentException(
            "No active reporter found for username: " + username));

    String code = generateCode();
    otpStore.put(username, new OtpEntry(code, Instant.now().plusSeconds(OTP_TTL_SECONDS)));
    whatsAppService.sendMessage(user.getPhoneNumber(),
        "Your Diocese Dashboard login code is: " + code
            + ". It expires in 10 minutes.");
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

  private String generateCode() {
    int code = secureRandom.nextInt(OTP_MODULUS);
    return String.format("%0" + OTP_DIGITS + "d", code);
  }

  /** Immutable OTP entry holding the code and its expiry instant. */
  record OtpEntry(String code, Instant expiry) {
  }
}
