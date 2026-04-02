package org.iecr.diocesedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

class PortraitServiceTest {

  private final PortraitService portraitService = new PortraitService();

  @Test
  void resolveCelebrantPortrait_usesNormalizedSlugForBundledPortraits() {
    PortraitService.PortraitAsset portrait = portraitService.resolveCelebrantPortrait("Ána Pérez");

    assertThat(new String(portrait.content(), StandardCharsets.UTF_8))
        .contains("Ana Perez Test Portrait");
    assertThat(portrait.mediaType()).isEqualTo(MediaType.valueOf("image/svg+xml"));
  }

  @Test
  void resolveChurchPortrait_fallsBackToPlaceholder() {
    PortraitService.PortraitAsset portrait = portraitService.resolveChurchPortrait("Missing Church");

    assertThat(new String(portrait.content(), StandardCharsets.UTF_8)).contains("Church Placeholder");
  }

  @Test
  void normalizeName_removesUnsafeCharacters() {
    assertThat(portraitService.normalizeName("../San José // Catedral"))
        .isEqualTo("san-jose-catedral");
  }

  @Test
  void buildCelebrantPortraitUrl_encodesTheNameForAStableEndpoint() {
    assertThat(portraitService.buildCelebrantPortraitUrl("Ana Pérez"))
        .isEqualTo("/api/portraits/celebrants?name=Ana+P%C3%A9rez");
  }
}
