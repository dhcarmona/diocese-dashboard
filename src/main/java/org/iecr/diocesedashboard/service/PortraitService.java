package org.iecr.diocesedashboard.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.text.Normalizer;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Resolves bundled Church and Celebrant portrait files from the WAR into
 * data URLs for the SPA while preventing path-based resource access.
 */
@Service
public class PortraitService {

  private static final String PORTRAITS_ROOT = "static/portraits/";
  private static final String PLACEHOLDER_SLUG = "placeholder";
  private static final String PLACEHOLDER_EXTENSION = "svg";
  private static final List<String> ALLOWED_EXTENSIONS =
      List.of("svg", "png", "jpg", "jpeg", "webp");
  private static final Map<String, String> MEDIA_TYPES = Map.of(
      "svg", "image/svg+xml",
      "png", "image/png",
      "jpg", "image/jpeg",
      "jpeg", "image/jpeg",
      "webp", "image/webp");
  private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}+");
  private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9]+");

  private final Map<String, String> portraitCache = new ConcurrentHashMap<>();

  /**
   * Resolves a bundled celebrant portrait or the celebrant placeholder.
   *
   * @param celebrantName the celebrant name used to derive the slug
   * @return base64 data URL for the portrait
   */
  public String resolveCelebrantPortraitDataUrl(String celebrantName) {
    return resolvePortraitDataUrl("celebrants", celebrantName);
  }

  /**
   * Resolves a bundled church portrait or the church placeholder.
   *
   * @param churchName the church name used to derive the slug
   * @return base64 data URL for the portrait
   */
  public String resolveChurchPortraitDataUrl(String churchName) {
    return resolvePortraitDataUrl("churches", churchName);
  }

  String normalizeName(String value) {
    if (value == null) {
      return "";
    }
    String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
    String withoutDiacritics = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
    String slug = NON_ALPHANUMERIC_PATTERN.matcher(
        withoutDiacritics.toLowerCase(Locale.ROOT).trim()).replaceAll("-");
    return slug.replaceAll("(^-+|-+$)", "");
  }

  private String resolvePortraitDataUrl(String group, String value) {
    String portraitPath = findPortraitPath(group, normalizeName(value));
    return portraitCache.computeIfAbsent(portraitPath, this::loadPortraitDataUrl);
  }

  private String findPortraitPath(String group, String slug) {
    if (!slug.isBlank()) {
      for (String extension : ALLOWED_EXTENSIONS) {
        String candidatePath = buildPortraitPath(group, slug, extension);
        if (new ClassPathResource(candidatePath).exists()) {
          return candidatePath;
        }
      }
    }
    return buildPortraitPath(group, PLACEHOLDER_SLUG, PLACEHOLDER_EXTENSION);
  }

  private String buildPortraitPath(String group, String slug, String extension) {
    return PORTRAITS_ROOT + group + "/" + slug + "." + extension;
  }

  private String loadPortraitDataUrl(String portraitPath) {
    ClassPathResource portraitResource = new ClassPathResource(portraitPath);
    try {
      byte[] content = FileCopyUtils.copyToByteArray(portraitResource.getInputStream());
      String extension = portraitPath.substring(portraitPath.lastIndexOf('.') + 1);
      String encodedContent = Base64.getEncoder().encodeToString(content);
      return "data:" + MEDIA_TYPES.get(extension) + ";base64," + encodedContent;
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load portrait resource: " + portraitPath, ex);
    }
  }
}
