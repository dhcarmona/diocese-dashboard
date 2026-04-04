package org.iecr.diocesedashboard.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Resolves bundled Church and Celebrant portrait files from the WAR while
 * preventing path-based resource access.
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

  private final Map<String, PortraitAsset> portraitCache = new ConcurrentHashMap<>();

  /**
   * Builds the stable internal URL for a celebrant portrait.
   *
   * @param celebrantName the celebrant name used to derive the slug
   * @return application URL for the portrait
   */
  public String buildCelebrantPortraitUrl(String celebrantName) {
    return buildPortraitUrl("/api/portraits/celebrants", celebrantName);
  }

  /**
   * Builds the stable internal URL for a church portrait.
   *
   * @param churchName the church name used to derive the slug
   * @return application URL for the portrait
   */
  public String buildChurchPortraitUrl(String churchName) {
    return buildPortraitUrl("/api/portraits/churches", churchName);
  }

  /**
   * Resolves the bytes and media type for a celebrant portrait.
   *
   * @param celebrantName the celebrant name used to derive the slug
   * @return portrait asset bytes and media type
   */
  public PortraitAsset resolveCelebrantPortrait(String celebrantName) {
    return resolvePortrait("celebrants", celebrantName);
  }

  /**
   * Resolves the bytes and media type for a church portrait.
   *
   * @param churchName the church name used to derive the slug
   * @return portrait asset bytes and media type
   */
  public PortraitAsset resolveChurchPortrait(String churchName) {
    return resolvePortrait("churches", churchName);
  }

  /**
   * Builds the stable internal URL for a service template banner.
   *
   * @param templateName the template name used to derive the slug
   * @return application URL for the banner
   */
  public String buildServiceTemplateBannerUrl(String templateName) {
    return buildPortraitUrl("/api/portraits/service-templates", templateName);
  }

  /**
   * Resolves the bytes and media type for a service template banner.
   *
   * @param templateName the template name used to derive the slug
   * @return portrait asset bytes and media type
   */
  public PortraitAsset resolveServiceTemplateBanner(String templateName) {
    return resolvePortrait("service-templates", templateName);
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

  private String buildPortraitUrl(String basePath, String entityName) {
    return basePath + "?name=" + URLEncoder.encode(
        entityName == null ? "" : entityName, StandardCharsets.UTF_8);
  }

  private PortraitAsset resolvePortrait(String group, String value) {
    String portraitPath = findPortraitPath(group, normalizeName(value));
    return portraitCache.computeIfAbsent(portraitPath, this::loadPortraitAsset);
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

  private PortraitAsset loadPortraitAsset(String portraitPath) {
    ClassPathResource portraitResource = new ClassPathResource(portraitPath);
    try {
      byte[] content = FileCopyUtils.copyToByteArray(portraitResource.getInputStream());
      String extension = portraitPath.substring(portraitPath.lastIndexOf('.') + 1);
      return new PortraitAsset(MediaType.parseMediaType(MEDIA_TYPES.get(extension)), content);
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load portrait resource: " + portraitPath, ex);
    }
  }

  public record PortraitAsset(MediaType mediaType, byte[] content) {
  }
}
