package org.iecr.diocesedashboard.service;

import org.iecr.diocesedashboard.domain.objects.ReporterLoginToken;
import org.iecr.diocesedashboard.domain.repositories.ReporterLoginTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/** Thin service for persisting and querying {@link ReporterLoginToken} entities. */
@Service
public class ReporterLoginTokenService {

  private final ReporterLoginTokenRepository repository;

  @Autowired
  public ReporterLoginTokenService(ReporterLoginTokenRepository repository) {
    this.repository = repository;
  }

  /**
   * Checks whether any token for the given username was created after the given threshold.
   *
   * @param username  the reporter's username
   * @param threshold the reference instant
   * @return true if a recently-created token exists
   */
  public boolean existsByUsernameAndCreatedAtAfter(String username, Instant threshold) {
    return repository.existsByUsernameAndCreatedAtAfter(username, threshold);
  }

  /**
   * Saves a new login token.
   *
   * @param token the token to persist
   * @return the saved token
   */
  public ReporterLoginToken save(ReporterLoginToken token) {
    return repository.save(token);
  }

  /**
   * Looks up a token by its token string.
   *
   * @param token the token string
   * @return the matching entity, or empty if not found
   */
  public Optional<ReporterLoginToken> findByToken(String token) {
    return repository.findByToken(token);
  }

  /**
   * Atomically deletes a token by its token string. Safe to call if the row is already gone.
   *
   * @param token the token string to invalidate
   */
  @Transactional
  public void deleteByToken(String token) {
    repository.deleteByTokenValue(token);
  }

  /**
   * Atomically claims (deletes) a token. Returns true if this call deleted the row,
   * false if another concurrent request already consumed it.
   *
   * @param token the token string to claim
   * @return true if successfully claimed
   */
  @Transactional
  public boolean claimToken(String token) {
    return repository.deleteByTokenValue(token) > 0;
  }

  /**
   * Deletes all tokens for the given username.
   *
   * @param username the reporter's username
   */
  @Transactional
  public void deleteByUsername(String username) {
    repository.deleteByUsername(username);
  }

  /**
   * Removes all tokens that expired before the given instant.
   *
   * @param cutoff expiry cutoff
   */
  @Transactional
  public void deleteExpiredBefore(Instant cutoff) {
    repository.deleteByExpiresAtBefore(cutoff);
  }
}
