package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Creates and validates short-lived tokens for public "open next pending link" actions.
 */
@Service
public class ReporterLinkFollowUpTokenService {

  private static final Duration TOKEN_TTL = Duration.ofMinutes(15);
  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private final Base64.Encoder base64UrlEncoder = Base64.getUrlEncoder().withoutPadding();
  private final Base64.Decoder base64UrlDecoder = Base64.getUrlDecoder();
  private final byte[] signingKey;

  /** Creates a signer with a random in-memory key for short-lived follow-up tokens. */
  public ReporterLinkFollowUpTokenService() {
    signingKey = new byte[32];
    new SecureRandom().nextBytes(signingKey);
  }

  /**
   * Creates a short-lived follow-up token for the given reporter.
   *
   * @param reporter the reporter allowed to resolve the next pending link
   * @return a signed token that can be used to look up the next pending link
   */
  public String createToken(DashboardUser reporter) {
    String payload = reporter.getId() + ":" + Instant.now().getEpochSecond();
    String encodedPayload = base64UrlEncoder.encodeToString(
        payload.getBytes(StandardCharsets.UTF_8));
    return encodedPayload + "." + sign(encodedPayload);
  }

  /**
   * Resolves a reporter ID from a previously issued follow-up token.
   *
   * @param token the signed follow-up token
   * @return the reporter ID when the token is valid and unexpired
   */
  public Optional<Long> resolveReporterId(String token) {
    String[] parts = token.split("\\.", 2);
    if (parts.length != 2) {
      return Optional.empty();
    }

    String encodedPayload = parts[0];
    String actualSignature = parts[1];
    String expectedSignature = sign(encodedPayload);
    if (!MessageDigest.isEqual(
        actualSignature.getBytes(StandardCharsets.UTF_8),
        expectedSignature.getBytes(StandardCharsets.UTF_8))) {
      return Optional.empty();
    }

    String payload;
    try {
      payload = new String(base64UrlDecoder.decode(encodedPayload), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }

    String[] payloadParts = payload.split(":", 2);
    if (payloadParts.length != 2) {
      return Optional.empty();
    }

    try {
      long reporterId = Long.parseLong(payloadParts[0]);
      long issuedAt = Long.parseLong(payloadParts[1]);
      Instant expiresAt = Instant.ofEpochSecond(issuedAt).plus(TOKEN_TTL);
      if (Instant.now().isAfter(expiresAt)) {
        return Optional.empty();
      }
      return Optional.of(reporterId);
    } catch (NumberFormatException ex) {
      return Optional.empty();
    }
  }

  private String sign(String encodedPayload) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
      byte[] signature = mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8));
      return base64UrlEncoder.encodeToString(signature);
    } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
      throw new IllegalStateException("Could not sign reporter-link follow-up token", ex);
    }
  }
}
