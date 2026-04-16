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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class WhatsAppServiceTest {

  private WhatsAppService service;

  @BeforeEach
  void setUp() {
    service = spy(new WhatsAppService(
        "https://graph.facebook.com",
        "v23.0",
        "test_access_token",
        "123456789",
        "en",
        "es",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        mock(WhatsAppMessageLogService.class)));
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
    WhatsAppService templatedService = spy(new WhatsAppService(
        "https://graph.facebook.com",
        "v23.0",
        "test_access_token",
        "123456789",
        "en_US",
        "es",
        "",
        "reporter_login_code_en",
        "reporter_login_code_es",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        mock(WhatsAppMessageLogService.class)));
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
  @SuppressWarnings("unchecked")
  void buildTemplatePayload_addsAuthenticationButtonParametersForOtpTemplates() throws Exception {
    Method buildTemplatePayload = WhatsAppService.class.getDeclaredMethod(
        "buildTemplatePayload",
        WhatsAppService.TemplateType.class,
        String.class,
        String.class,
        Map.class);
    buildTemplatePayload.setAccessible(true);

    Map<String, Object> payload = (Map<String, Object>) buildTemplatePayload.invoke(
        service,
        WhatsAppService.TemplateType.OTP_AUTHENTICATION,
        "reporter_login_code_en",
        "en_US",
        Map.of("1", "123456"));

    List<Map<String, Object>> components = (List<Map<String, Object>>) payload.get("components");
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
  @SuppressWarnings("unchecked")
  void buildTemplatePayload_keepsUtilityTemplatesAsBodyOnlyParameters() throws Exception {
    Method buildTemplatePayload = WhatsAppService.class.getDeclaredMethod(
        "buildTemplatePayload",
        WhatsAppService.TemplateType.class,
        String.class,
        String.class,
        Map.class);
    buildTemplatePayload.setAccessible(true);

    Map<String, Object> payload = (Map<String, Object>) buildTemplatePayload.invoke(
        service,
        WhatsAppService.TemplateType.REPORTER_LINK,
        "reporter_link_en",
        "en",
        Map.of("2", "St Mark Church", "1", "Sunday Eucharist"));

    List<Map<String, Object>> components = (List<Map<String, Object>>) payload.get("components");
    assertEquals(1, components.size());
    assertEquals("body", components.get(0).get("type"));
    assertEquals(
        List.of(
            Map.of("type", "text", "text", "Sunday Eucharist"),
            Map.of("type", "text", "text", "St Mark Church")),
        components.get(0).get("parameters"));
  }
}
