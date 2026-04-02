package org.iecr.diocesedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

class PortraitServiceTest {

  private final PortraitService portraitService = new PortraitService();

  @Test
  void resolveCelebrantPortraitDataUrl_usesNormalizedSlugForBundledPortraits() {
    String portraitDataUrl = portraitService.resolveCelebrantPortraitDataUrl("Ána Pérez");

    assertThat(decodeDataUrl(portraitDataUrl)).contains("Ana Perez Test Portrait");
  }

  @Test
  void resolveChurchPortraitDataUrl_fallsBackToPlaceholder() {
    String portraitDataUrl = portraitService.resolveChurchPortraitDataUrl("Missing Church");

    assertThat(decodeDataUrl(portraitDataUrl)).contains("Church Placeholder");
  }

  @Test
  void normalizeName_removesUnsafeCharacters() {
    assertThat(portraitService.normalizeName("../San José // Catedral"))
        .isEqualTo("san-jose-catedral");
  }

  private String decodeDataUrl(String dataUrl) {
    String base64Payload = dataUrl.substring(dataUrl.indexOf(',') + 1);
    return new String(Base64.getDecoder().decode(base64Payload), StandardCharsets.UTF_8);
  }
}
