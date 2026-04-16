package org.iecr.diocesedashboard.domain.objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import java.time.Instant;

/**
 * A short-lived login token for reporter users that authenticates via a magic link sent to
 * their WhatsApp number. Tokens are single-use and expire after a fixed TTL.
 */
@Entity
public class ReporterLoginToken {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reporter_login_token_seq")
  @SequenceGenerator(name = "reporter_login_token_seq", allocationSize = 50)
  private Long id;

  @Column(unique = true, nullable = false)
  private String token;

  @Column(nullable = false)
  private String username;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  /** Default no-arg constructor required by JPA. */
  public ReporterLoginToken() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  @Override
  public String toString() {
    return "ReporterLoginToken{id=" + id + ", username='" + username + "'}";
  }
}
