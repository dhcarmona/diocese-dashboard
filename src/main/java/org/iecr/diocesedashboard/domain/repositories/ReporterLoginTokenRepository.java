package org.iecr.diocesedashboard.domain.repositories;

import org.iecr.diocesedashboard.domain.objects.ReporterLoginToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/** Spring Data repository for {@link ReporterLoginToken} entities. */
public interface ReporterLoginTokenRepository extends JpaRepository<ReporterLoginToken, Long> {

  /**
   * Finds a login token by its token string.
   *
   * @param token the token string to look up
   * @return the matching token, or empty if not found
   */
  Optional<ReporterLoginToken> findByToken(String token);

  /**
   * Checks whether any token exists for the given username created after the given instant.
   * Used to enforce a minimum interval between requests.
   *
   * @param username  the reporter's username
   * @param threshold tokens created after this instant are considered recent
   * @return true if a recent token exists
   */
  boolean existsByUsernameAndCreatedAtAfter(String username, Instant threshold);

  /**
   * Used to invalidate any prior tokens when a new one is requested.
   *
   * @param username the reporter's username
   */
  void deleteByUsername(String username);

  /**
   * Atomically deletes a token by its token string. Returns the number of rows deleted.
   * Uses a bulk JPQL delete so it is idempotent — safe to call even if the row is already gone.
   *
   * @param token the token string to delete
   * @return number of rows deleted (1 if the token existed, 0 otherwise)
   */
  @Modifying
  @Query("DELETE FROM ReporterLoginToken t WHERE t.token = :token")
  int deleteByTokenValue(@Param("token") String token);

  /**
   * Deletes all tokens that expired before the given instant.
   *
   * @param cutoff tokens expiring before this instant are deleted
   */
  void deleteByExpiresAtBefore(Instant cutoff);
}
