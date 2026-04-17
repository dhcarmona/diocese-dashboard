package org.iecr.diocesedashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class WhatsAppServiceTest {

  private WhatsAppService service;

  private static WhatsAppMetaProperties defaultProps() {
    WhatsAppMetaProperties props = new WhatsAppMetaProperties();
    return props;
  }

  @BeforeEach
  void setUp() {
    service = spy(new WhatsAppService(defaultProps(), mock(WhatsAppMessageLogService.class)));
  }

  @Test
  void sendMessage_normalizesRecipientForMetaApi() {
    doNothing().when(service).dispatchTextMessage(any(), any());

    service.sendMessage("+506 8888-8888", "Hello from the dashboard!");

    verify(service).dispatchTextMessage(
        "50688888888",
        "Hello from the dashboard!"
    );
  }

  @Test
  void sendConfiguredMessage_usesLocalizedTemplateWhenConfigured() {
    WhatsAppMetaProperties props = new WhatsAppMetaProperties();
    props.getLanguageCode().setEn("en_US");
    props.getTemplates().getOtpAuthentication().setEn("reporter_login_code_en");
    props.getTemplates().getOtpAuthentication().setEs("reporter_login_code_es");

    WhatsAppService templatedService = spy(
        new WhatsAppService(props, mock(WhatsAppMessageLogService.class)));
    doNothing().when(templatedService).dispatchTemplateMessage(any(), any(), any(), any(), any());

    templatedService.sendConfiguredMessage(
        "+50688888888",
        "fallback body",
        Locale.ENGLISH,
        WhatsAppService.TemplateType.OTP_AUTHENTICATION,
        Map.of("1", "123456"));

    verify(templatedService).dispatchTemplateMessage(
        "50688888888",
        "reporter_login_code_en",
        "en_US",
        WhatsAppService.TemplateType.OTP_AUTHENTICATION,
        Map.of("1", "123456")
    );
  }

  @Test
  void buildComponents_addsAuthenticationButtonParametersForOtpTemplates() {
    List<Map<String, Object>> components = service.buildComponents(
        WhatsAppService.TemplateType.OTP_AUTHENTICATION,
        Map.of("1", "123456"));

    assertEquals(2, components.size());
    assertEquals("body", components.get(0).get("type"));
    assertEquals(
        List.of(Map.of("type", "text", "text", "123456")),
        components.get(0).get("parameters"));
    assertEquals("button", components.get(1).get("type"));
    assertEquals("url", components.get(1).get("sub_type"));
    assertEquals("0", components.get(1).get("index"));
    assertEquals(
        List.of(Map.of("type", "text", "text", "123456")),
        components.get(1).get("parameters"));
  }

  @Test
  void buildComponents_keepsUtilityTemplatesAsBodyOnlyParameters() {
    List<Map<String, Object>> components = service.buildComponents(
        WhatsAppService.TemplateType.REPORTER_LINK,
        Map.of("2", "St Mark Church", "1", "Sunday Eucharist"));

    assertEquals(1, components.size());
    assertEquals("body", components.get(0).get("type"));
    assertEquals(
        List.of(
            Map.of("type", "text", "text", "Sunday Eucharist"),
            Map.of("type", "text", "text", "St Mark Church")),
        components.get(0).get("parameters"));
  }
}
