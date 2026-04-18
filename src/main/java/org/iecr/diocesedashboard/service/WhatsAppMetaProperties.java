package org.iecr.diocesedashboard.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Typed configuration for the Meta WhatsApp Cloud API integration.
 *
 * <p>All properties are bound from the {@code whatsapp.meta} prefix.
 * Template names are optional; when omitted the service falls back to free-form text.
 */
@Component
@ConfigurationProperties(prefix = "whatsapp.meta")
public class WhatsAppMetaProperties {

  private String baseUrl = "https://graph.facebook.com";
  private String apiVersion = "v25.0";
  private String accessToken = "";
  private String phoneNumberId = "";
  private LanguageCode languageCode = new LanguageCode();
  private Templates templates = new Templates();

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getPhoneNumberId() {
    return phoneNumberId;
  }

  public void setPhoneNumberId(String phoneNumberId) {
    this.phoneNumberId = phoneNumberId;
  }

  public LanguageCode getLanguageCode() {
    return languageCode;
  }

  public void setLanguageCode(LanguageCode languageCode) {
    this.languageCode = languageCode;
  }

  public Templates getTemplates() {
    return templates;
  }

  public void setTemplates(Templates templates) {
    this.templates = templates;
  }

  /** Spanish language code sent to the Meta API with each template request. */
  public static class LanguageCode {
    private String es = "es";

    public String getEs() {
      return es;
    }

    public void setEs(String es) {
      this.es = es;
    }
  }

  /** Holds locale-specific template names for every supported message type. */
  public static class Templates {
    private TemplateNameSet otpAuthentication = new TemplateNameSet();
    private TemplateNameSet reporterWelcome = new TemplateNameSet();
    private TemplateNameSet reporterLink = new TemplateNameSet();
    private TemplateNameSet reportSubmitted = new TemplateNameSet();
    private TemplateNameSet reportUpdated = new TemplateNameSet();
    private TemplateNameSet reportDeleted = new TemplateNameSet();
    private TemplateNameSet reporterLoginLink = new TemplateNameSet();

    public TemplateNameSet getOtpAuthentication() {
      return otpAuthentication;
    }

    public void setOtpAuthentication(TemplateNameSet ss) {
      otpAuthentication = ss;
    }

    public TemplateNameSet getReporterWelcome() {
      return reporterWelcome;
    }

    public void setReporterWelcome(TemplateNameSet ss) {
      reporterWelcome = ss;
    }

    public TemplateNameSet getReporterLink() {
      return reporterLink;
    }

    public void setReporterLink(TemplateNameSet ss) {
      reporterLink = ss;
    }

    public TemplateNameSet getReportSubmitted() {
      return reportSubmitted;
    }

    public void setReportSubmitted(TemplateNameSet ss) {
      reportSubmitted = ss;
    }

    public TemplateNameSet getReportUpdated() {
      return reportUpdated;
    }

    public void setReportUpdated(TemplateNameSet ss) {
      reportUpdated = ss;
    }

    public TemplateNameSet getReportDeleted() {
      return reportDeleted;
    }

    public void setReportDeleted(TemplateNameSet ss) {
      reportDeleted = ss;
    }

    public TemplateNameSet getReporterLoginLink() {
      return reporterLoginLink;
    }

    public void setReporterLoginLink(TemplateNameSet ss) {
      reporterLoginLink = ss;
    }
  }

  /** Fallback and Spanish template name variants for a single message type. */
  public static class TemplateNameSet {
    private String fallback = "";
    private String es = "";

    public String getFallback() {
      return fallback;
    }

    public void setFallback(String fallback) {
      this.fallback = fallback;
    }

    public String getEs() {
      return es;
    }

    public void setEs(String es) {
      this.es = es;
    }
  }
}
