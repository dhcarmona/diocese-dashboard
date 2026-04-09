package org.iecr.diocesedashboard.domain.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ReporterLink;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@DataJpaTest
class ReporterLinkRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private ReporterLinkRepository reporterLinkRepository;

  private DashboardUser reporter;
  private Church church;
  private Church secondChurch;
  private ServiceTemplate template;

  @BeforeEach
  void setUp() {
    church = new Church();
    church.setName("Trinity");
    entityManager.persist(church);
    secondChurch = new Church();
    secondChurch.setName("StPaul");
    entityManager.persist(secondChurch);

    reporter = new DashboardUser();
    reporter.setUsername("reporter1");
    reporter.setPasswordHash("$2a$10$hash");
    reporter.setRole(UserRole.REPORTER);
    reporter.setEnabled(true);
    reporter.setAssignedChurches(Set.of(church, secondChurch));
    entityManager.persist(reporter);

    template = new ServiceTemplate();
    template.setServiceTemplateName("Sunday Mass");
    entityManager.persist(template);

    entityManager.flush();
  }

  private ReporterLink buildLink(String token) {
    return buildLink(token, church);
  }

  private ReporterLink buildLink(String token, Church reporterChurch) {
    ReporterLink link = new ReporterLink();
    link.setToken(token);
    link.setReporter(reporter);
    link.setChurch(reporterChurch);
    link.setServiceTemplate(template);
    link.setActiveDate(LocalDate.now());
    return link;
  }

  @Test
  void save_persistsAndAssignsId() {
    ReporterLink saved = reporterLinkRepository.save(buildLink(UUID.randomUUID().toString()));

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getReporter().getUsername()).isEqualTo("reporter1");
  }

  @Test
  void findByToken_returnsPresent_whenExists() {
    String token = UUID.randomUUID().toString();
    entityManager.persist(buildLink(token));
    entityManager.flush();

    Optional<ReporterLink> result = reporterLinkRepository.findByToken(token);

    assertThat(result).isPresent();
    assertThat(result.get().getToken()).isEqualTo(token);
  }

  @Test
  void findByToken_returnsEmpty_whenNotExists() {
    Optional<ReporterLink> result = reporterLinkRepository.findByToken("nonexistent-token");

    assertThat(result).isEmpty();
  }

  @Test
  void existsByToken_returnsTrue_whenExists() {
    String token = UUID.randomUUID().toString();
    entityManager.persist(buildLink(token));
    entityManager.flush();

    assertThat(reporterLinkRepository.existsByToken(token)).isTrue();
  }

  @Test
  void existsByToken_returnsFalse_whenNotExists() {
    assertThat(reporterLinkRepository.existsByToken("no-such-token")).isFalse();
  }

  @Test
  void deleteByToken_removesLink() {
    String token = UUID.randomUUID().toString();
    entityManager.persist(buildLink(token));
    entityManager.flush();

    reporterLinkRepository.deleteByToken(token);
    entityManager.flush();

    assertThat(reporterLinkRepository.findByToken(token)).isEmpty();
  }

  @Test
  void findAll_returnsAllLinks() {
    entityManager.persist(buildLink(UUID.randomUUID().toString()));
    entityManager.persist(buildLink(UUID.randomUUID().toString()));
    entityManager.flush();

    assertThat(reporterLinkRepository.findAll()).hasSize(2);
  }

  @Test
  void findAll_allowsSameReporterAndTemplateAcrossDifferentChurches() {
    entityManager.persist(buildLink(UUID.randomUUID().toString(), church));
    entityManager.persist(buildLink(UUID.randomUUID().toString(), secondChurch));
    entityManager.flush();

    assertThat(reporterLinkRepository.findAll())
        .extracting(link -> link.getChurch().getName())
        .containsExactlyInAnyOrder("Trinity", "StPaul");
  }

  @Test
  void findByChurchAndServiceTemplate_returnsOnlyMatchingLinks() {
    ServiceTemplate otherTemplate = new ServiceTemplate();
    otherTemplate.setServiceTemplateName("Evening Prayer");
    entityManager.persist(otherTemplate);
    entityManager.flush();

    entityManager.persist(buildLink(UUID.randomUUID().toString(), church));
    ReporterLink wrongChurch = buildLink(UUID.randomUUID().toString(), secondChurch);
    entityManager.persist(wrongChurch);
    ReporterLink wrongTemplate = new ReporterLink();
    wrongTemplate.setToken(UUID.randomUUID().toString());
    wrongTemplate.setReporter(reporter);
    wrongTemplate.setChurch(church);
    wrongTemplate.setServiceTemplate(otherTemplate);
    wrongTemplate.setActiveDate(LocalDate.now());
    entityManager.persist(wrongTemplate);
    entityManager.flush();

    List<ReporterLink> result =
        reporterLinkRepository.findByChurchAndServiceTemplate(church, template);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getChurch().getName()).isEqualTo("Trinity");
    assertThat(result.get(0).getServiceTemplate().getServiceTemplateName())
        .isEqualTo("Sunday Mass");
  }

  @Test
  void findByChurchInAndServiceTemplate_returnsLinksForMultipleChurches() {
    entityManager.persist(buildLink(UUID.randomUUID().toString(), church));
    entityManager.persist(buildLink(UUID.randomUUID().toString(), secondChurch));
    entityManager.flush();

    List<ReporterLink> result = reporterLinkRepository.findByChurchInAndServiceTemplate(
        List.of(church, secondChurch), template);

    assertThat(result).hasSize(2);
    assertThat(result).extracting(l -> l.getChurch().getName())
        .containsExactlyInAnyOrder("Trinity", "StPaul");
  }

  @Test
  void findByChurchInAndServiceTemplate_excludesOtherTemplate() {
    ServiceTemplate other = new ServiceTemplate();
    other.setServiceTemplateName("Vespers");
    entityManager.persist(other);
    entityManager.flush();

    entityManager.persist(buildLink(UUID.randomUUID().toString(), church));
    ReporterLink wrongTemplate = new ReporterLink();
    wrongTemplate.setToken(UUID.randomUUID().toString());
    wrongTemplate.setReporter(reporter);
    wrongTemplate.setChurch(church);
    wrongTemplate.setServiceTemplate(other);
    wrongTemplate.setActiveDate(LocalDate.now());
    entityManager.persist(wrongTemplate);
    entityManager.flush();

    List<ReporterLink> result = reporterLinkRepository.findByChurchInAndServiceTemplate(
        List.of(church), template);

    assertThat(result).hasSize(1);
  }
}
