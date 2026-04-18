package org.iecr.diocesedashboard.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for sending WhatsApp messages via the Meta Cloud API.
 *
 * <p>The following environment variables should be configured:
 * <ul>
 *   <li>{@code WHATSAPP_ACCESS_TOKEN} — Meta system-user access token</li>
 *   <li>{@code WHATSAPP_PHONE_NUMBER_ID} — WhatsApp business phone number ID</li>
 *   <li>{@code WHATSAPP_TEMPLATE_*} — optional approved WhatsApp template names</li>
 * </ul>
 *
 * <p>When an approved template name is configured, the service sends a template message through
 * the Meta Cloud API so business-initiated delivery remains available outside the
 * 24-hour customer service window. Otherwise, it falls back to a free-form WhatsApp text
 * message, which only works inside the open customer service window.
 */
@Service
public class WhatsAppService {

  private static final Logger LOG = LoggerFactory.getLogger(WhatsAppService.class);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

  /** Supported outbound WhatsApp template types. */
  public enum TemplateType {
    OTP_AUTHENTICATION,
    REPORTER_WELCOME,
    REPORTER_LINK,
    REPORT_SUBMITTED,
    REPORT_UPDATED,
    REPORT_DELETED,
    REPORTER_LOGIN_LINK
  }

  private final String baseUrl;
  private final String apiVersion;
  private final String accessToken;
  private final String phoneNumberId;
  private final String spanishLanguageCode;
  private final Map<TemplateType, TemplateNameSet> templateNameSets;
  private final WhatsAppMessageLogService messageLogService;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  public WhatsAppService(WhatsAppMetaProperties props, WhatsAppMessageLogService messageLogService) {
    this.baseUrl = props.getBaseUrl();
    this.apiVersion = props.getApiVersion();
    this.accessToken = props.getAccessToken();
    this.phoneNumberId = props.getPhoneNumberId();
    this.spanishLanguageCode = props.getLanguageCode().getEs();
    this.templateNameSets = Map.ofEntries(
        Map.entry(TemplateType.OTP_AUTHENTICATION,
            toTemplateNameSet(props.getTemplates().getOtpAuthentication())),
        Map.entry(TemplateType.REPORTER_WELCOME,
            toTemplateNameSet(props.getTemplates().getReporterWelcome())),
        Map.entry(TemplateType.REPORTER_LINK,
            toTemplateNameSet(props.getTemplates().getReporterLink())),
        Map.entry(TemplateType.REPORT_SUBMITTED,
            toTemplateNameSet(props.getTemplates().getReportSubmitted())),
        Map.entry(TemplateType.REPORT_UPDATED,
            toTemplateNameSet(props.getTemplates().getReportUpdated())),
        Map.entry(TemplateType.REPORT_DELETED,
            toTemplateNameSet(props.getTemplates().getReportDeleted())),
        Map.entry(TemplateType.REPORTER_LOGIN_LINK,
            toTemplateNameSet(props.getTemplates().getReporterLoginLink())));
    this.messageLogService = messageLogService;
    this.httpClient = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
  }

  private static TemplateNameSet toTemplateNameSet(WhatsAppMetaProperties.TemplateNameSet ss) {
    return new TemplateNameSet(ss.getFallback(), ss.getEs());
  }

  /**
   * Sends a WhatsApp message to the given phone number.
   *
   * @param to   recipient phone number in E.164 format (e.g. {@code +50688888888})
   * @param body the message text
   */
  public void sendMessage(String to, String body) {
    dispatchTextMessage(normalizeRecipient(to), body);
  }

  /**
   * Sends either a configured WhatsApp template or the provided free-form message body.
   *
   * @param to                recipient phone number in E.164 format
   * @param body              fallback message body used when no template name is configured
   * @param templateType      the logical template to use when configured
   * @param templateVariables placeholder values keyed by WhatsApp variable number
   */
  public void sendConfiguredMessage(String to, String body,
      TemplateType templateType, Map<String, String> templateVariables) {
    TemplateRequest templateRequest = getTemplateRequest(templateType);
    if (templateRequest != null) {
      dispatchTemplateMessage(
          normalizeRecipient(to),
          templateRequest.templateName(),
          templateRequest.languageCode(),
          templateType,
          templateVariables);
      return;
    }
    sendMessage(to, body);
  }

  /**
   * Sends either a configured template or a free-form WhatsApp message and records the body.
   *
   * @param to                recipient phone number in E.164 format
   * @param body              the message body or fallback free-form text
   * @param recipientUsername the dashboard username of the recipient
   * @param templateType      the logical template to use when configured
   * @param templateVariables placeholder values keyed by WhatsApp variable number
   */
  public void sendConfiguredMessageAndLog(String to, String body, String recipientUsername,
      TemplateType templateType, Map<String, String> templateVariables) {
    sendConfiguredMessageAndLog(
        to,
        body,
        body,
        recipientUsername,
        templateType,
        templateVariables);
  }

  /**
   * Sends either a configured template or a free-form WhatsApp message and records a summary.
   * Use this when the message body contains sensitive data (for example URLs with tokens)
   * that should not be stored verbatim.
   *
   * @param to                recipient phone number in E.164 format
   * @param body              the message body or fallback free-form text
   * @param logSummary        the redacted summary to store in the log
   * @param recipientUsername the dashboard username of the recipient
   * @param templateType      the logical template to use when configured
   * @param templateVariables placeholder values keyed by WhatsApp variable number
   */
  public void sendConfiguredMessageAndLog(String to, String body, String logSummary,
      String recipientUsername, TemplateType templateType,
      Map<String, String> templateVariables) {
    sendConfiguredMessage(to, body, templateType, templateVariables);
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
   * Sends an OTP WhatsApp message using the configured authentication template when available,
   * and records it in the message log without storing the code-bearing content.
   *
   * @param to                recipient phone number in E.164 format
   * @param body              the fallback OTP message text (not persisted)
   * @param code              the one-time passcode sent to the reporter
   * @param recipientUsername the dashboard username of the recipient
   */
  public void sendOtpAndLog(String to, String body, String code, String recipientUsername) {
    sendConfiguredMessage(to, body, TemplateType.OTP_AUTHENTICATION, Map.of("1", code));
    tryLogOtp(recipientUsername);
  }

  private void tryLogMessage(String recipientUsername, String body) {
    try {
      messageLogService.logMessage(recipientUsername, body);
    } catch (Exception ex) {
      LOG.warn("Failed to log WhatsApp message for {}", recipientUsername, ex);
    }
  }

  private void tryLogOtp(String recipientUsername) {
    try {
      messageLogService.logOtp(recipientUsername);
    } catch (Exception ex) {
      LOG.warn("Failed to log WhatsApp OTP for {}", recipientUsername, ex);
    }
  }

  /** Dispatches a free-form text message via the Meta Cloud API; package-private for tests. */
  void dispatchTextMessage(String normalizedTo, String body) {
    LOG.debug("Sending WhatsApp free-form text message to {}", normalizedTo);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("messaging_product", "whatsapp");
    payload.put("recipient_type", "individual");
    payload.put("to", normalizedTo);
    payload.put("type", "text");
    payload.put("text", Map.of("body", body));
    sendApiRequest(serializePayload(payload));
  }

  /** Dispatches a template message via the Meta Cloud API; package-private for tests. */
  void dispatchTemplateMessage(String normalizedTo, String templateName, String languageCode,
      TemplateType templateType, Map<String, String> templateVariables) {
    LOG.debug("Sending WhatsApp template message to {} using template '{}' (type={}, lang={})",
        normalizedTo, templateName, templateType, languageCode);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("messaging_product", "whatsapp");
    payload.put("recipient_type", "individual");
    payload.put("to", normalizedTo);
    payload.put("type", "template");
    payload.put(
        "template",
        buildTemplatePayload(templateType, templateName, languageCode, templateVariables));
    sendApiRequest(serializePayload(payload));
  }

  private Map<String, Object> buildTemplatePayload(TemplateType templateType, String templateName,
      String languageCode, Map<String, String> templateVariables) {
    Map<String, Object> templatePayload = new LinkedHashMap<>();
    templatePayload.put("name", templateName);
    templatePayload.put("language", Map.of("code", languageCode));
    List<Map<String, Object>> components = buildComponents(templateType, templateVariables);
    if (!components.isEmpty()) {
      templatePayload.put("components", components);
    }
    return templatePayload;
  }

  List<Map<String, Object>> buildComponents(
      TemplateType templateType, Map<String, String> templateVariables) {
    if (templateVariables == null || templateVariables.isEmpty()) {
      return List.of();
    }
    if (templateType == TemplateType.OTP_AUTHENTICATION) {
      return buildAuthenticationComponents(templateVariables);
    }
    if (templateType == TemplateType.REPORTER_LOGIN_LINK) {
      return buildLoginLinkComponents(templateVariables);
    }
    List<Map<String, Object>> parameters = new ArrayList<>();
    templateVariables.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(entry -> parameters.add(Map.of("type", "text", "text", entry.getValue())));
    return List.of(Map.of("type", "body", "parameters", parameters));
  }

  private List<Map<String, Object>> buildAuthenticationComponents(
      Map<String, String> templateVariables) {
    String code = templateVariables.get("1");
    if (!hasText(code)) {
      throw new IllegalArgumentException(
          "WhatsApp authentication templates require template variable 1 for the OTP code.");
    }
    Map<String, Object> parameter = Map.of("type", "text", "text", code);
    return List.of(
        Map.of("type", "body", "parameters", List.of(parameter)),
        Map.of(
            "type", "button",
            "sub_type", "url",
            "index", "0",
            "parameters", List.of(parameter)));
  }

  private List<Map<String, Object>> buildLoginLinkComponents(
      Map<String, String> templateVariables) {
    String token = templateVariables.get("1");
    if (!hasText(token)) {
      throw new IllegalArgumentException(
          "WhatsApp login link templates require template variable 1 for the token.");
    }
    return List.of(
        Map.of(
            "type", "button",
            "sub_type", "url",
            "index", "0",
            "parameters", List.of(Map.of("type", "text", "text", token))));
  }

  private void sendApiRequest(String payload) {
    if (!hasText(accessToken)) {
      throw new IllegalStateException("WHATSAPP_ACCESS_TOKEN is not configured.");
    }
    if (!hasText(phoneNumberId)) {
      throw new IllegalStateException("WHATSAPP_PHONE_NUMBER_ID is not configured.");
    }
    HttpRequest request = HttpRequest.newBuilder(buildMessagesUri())
        .timeout(REQUEST_TIMEOUT)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .POST(HttpRequest.BodyPublishers.ofString(payload))
        .build();
    try {
      HttpResponse<String> response = httpClient.send(
          request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException(
            "Meta WhatsApp API request failed with status "
                + response.statusCode() + ": " + response.body());
      }
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to call the Meta WhatsApp API.", ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Meta WhatsApp API request was interrupted.", ex);
    }
  }

  private URI buildMessagesUri() {
    String normalizedBaseUrl = trimTrailingSlash(baseUrl);
    String normalizedVersion = trimSlashes(apiVersion);
    String normalizedPhoneNumberId = trimSlashes(phoneNumberId);
    return URI.create(
        normalizedBaseUrl + "/" + normalizedVersion + "/" + normalizedPhoneNumberId + "/messages");
  }

  private TemplateRequest getTemplateRequest(TemplateType templateType) {
    TemplateNameSet templateNameSet = templateNameSets.get(templateType);
    if (templateNameSet == null) {
      return null;
    }
    if (hasText(templateNameSet.spanish())) {
      return new TemplateRequest(templateNameSet.spanish(), spanishLanguageCode);
    }
    if (!hasText(templateNameSet.fallback())) {
      return null;
    }
    return new TemplateRequest(templateNameSet.fallback(), spanishLanguageCode);
  }

  private String serializePayload(Map<String, Object> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize WhatsApp payload.", ex);
    }
  }

  private String normalizeRecipient(String to) {
    String normalized = to == null ? "" : to.replaceAll("[^0-9]", "");
    if (!hasText(normalized)) {
      throw new IllegalArgumentException("WhatsApp recipient phone number must not be blank.");
    }
    return normalized;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private String trimTrailingSlash(String value) {
    if (value == null) {
      return "";
    }
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }

  private String trimSlashes(String value) {
    if (value == null) {
      return "";
    }
    int start = 0;
    int end = value.length();
    while (start < end && value.charAt(start) == '/') {
      start++;
    }
    while (end > start && value.charAt(end - 1) == '/') {
      end--;
    }
    return value.substring(start, end);
  }

  private record TemplateNameSet(String fallback, String spanish) {
  }

  private record TemplateRequest(String templateName, String languageCode) {
  }
}
