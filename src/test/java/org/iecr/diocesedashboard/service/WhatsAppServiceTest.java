package org.iecr.diocesedashboard.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WhatsAppServiceTest {

  private WhatsAppService service;

  @BeforeEach
  void setUp() {
    service = spy(new WhatsAppService("test_sid", "test_token", "+14155238886"));
  }

  @Test
  void sendMessage_appliesWhatsappPrefixToBothNumbers() {
    doNothing().when(service).dispatchMessage(any(), any(), any());

    service.sendMessage("+50688888888", "Hello from the dashboard!");

    verify(service).dispatchMessage(
        "whatsapp:+14155238886",
        "whatsapp:+50688888888",
        "Hello from the dashboard!"
    );
  }

  @Test
  void sendMessage_usesConfiguredFromNumber() {
    WhatsAppService customService = spy(
        new WhatsAppService("test_sid", "test_token", "+50600000000"));
    doNothing().when(customService).dispatchMessage(any(), any(), any());

    customService.sendMessage("+50688888888", "Test");

    verify(customService).dispatchMessage(
        "whatsapp:+50600000000",
        "whatsapp:+50688888888",
        "Test"
    );
  }
}
