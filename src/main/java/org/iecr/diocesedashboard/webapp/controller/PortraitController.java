package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.service.PortraitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/** Serves bundled church and celebrant portraits through guarded API endpoints. */
@RestController
@RequestMapping("/api/portraits")
public class PortraitController {

  private static final Duration PORTRAIT_CACHE_DURATION = Duration.ofHours(1);

  private final PortraitService portraitService;

  @Autowired
  public PortraitController(PortraitService portraitService) {
    this.portraitService = portraitService;
  }

  /**
   * Returns the bundled portrait bytes for a celebrant name.
   *
   * @param name the celebrant name
   * @return portrait bytes and content type
   */
  @GetMapping("/celebrants")
  public ResponseEntity<byte[]> getCelebrantPortrait(@RequestParam String name) {
    return buildPortraitResponse(portraitService.resolveCelebrantPortrait(name));
  }

  /**
   * Returns the bundled portrait bytes for a church name.
   *
   * @param name the church name
   * @return portrait bytes and content type
   */
  @GetMapping("/churches")
  public ResponseEntity<byte[]> getChurchPortrait(@RequestParam String name) {
    return buildPortraitResponse(portraitService.resolveChurchPortrait(name));
  }

  /**
   * Returns the bundled banner bytes for a service template name.
   *
   * @param name the service template name
   * @return banner bytes and content type
   */
  @GetMapping("/service-templates")
  public ResponseEntity<byte[]> getServiceTemplateBanner(@RequestParam String name) {
    return buildPortraitResponse(portraitService.resolveServiceTemplateBanner(name));
  }

  private ResponseEntity<byte[]> buildPortraitResponse(PortraitService.PortraitAsset portrait) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(PORTRAIT_CACHE_DURATION).cachePrivate())
        .contentType(portrait.mediaType())
        .body(portrait.content());
  }
}
