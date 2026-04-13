package org.iecr.diocesedashboard.domain.repositories;

import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.ReporterLink;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Repository for {@link ReporterLink} persistence. */
public interface ReporterLinkRepository extends JpaRepository<ReporterLink, Long> {

  /**
   * Finds a reporter link by its token.
   *
   * @param token the unique token string
   * @return an {@link Optional} containing the link if found
   */
  Optional<ReporterLink> findByToken(String token);

  /**
   * Checks whether a link with the given token already exists.
   *
   * @param token the token to check
   * @return true if a link with that token exists
   */
  boolean existsByToken(String token);

  /**
   * Deletes the link with the given token.
   *
   * @param token the token of the link to delete
   */
  void deleteByToken(String token);

  /**
   * Atomically deletes the link with the given token and returns the number of rows deleted.
   * Used to implement single-use token semantics without a race condition.
   *
   * @param token the token of the link to delete
   * @return 1 if the token existed and was deleted, 0 if it was not found
   */
  @Modifying
  @Query("DELETE FROM ReporterLink r WHERE r.token = :token")
  int deleteByTokenReturningCount(@Param("token") String token);

  /**
   * Finds all reporter links for a given church and service template.
   *
   * @param church          the church to filter by
   * @param serviceTemplate the service template to filter by
   * @return list of matching reporter links
   */
  List<ReporterLink> findByChurchAndServiceTemplate(Church church, ServiceTemplate serviceTemplate);

  /**
   * Finds all reporter links for any of the given churches and a given service template.
   *
   * @param churches        the churches to filter by
   * @param serviceTemplate the service template to filter by
   * @return list of matching reporter links
   */
  List<ReporterLink> findByChurchInAndServiceTemplate(
      Iterable<Church> churches, ServiceTemplate serviceTemplate);
}
