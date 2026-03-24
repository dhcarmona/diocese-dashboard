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
}
