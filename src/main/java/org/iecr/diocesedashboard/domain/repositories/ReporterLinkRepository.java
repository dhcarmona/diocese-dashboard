package org.iecr.diocesedashboard.domain.repositories;

import org.iecr.diocesedashboard.domain.objects.ReporterLink;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
