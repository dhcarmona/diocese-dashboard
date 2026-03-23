package org.iecr.diocesedashboard.domain.objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/** A user account for accessing the diocese dashboard. */
@Entity
@Table(name = "DashboardUser")
public class DashboardUser {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dashboard_user_seq")
  @SequenceGenerator(name = "dashboard_user_seq", allocationSize = 50)
  private Long id;

  @Column(unique = true, nullable = false)
  private String username;

  @JsonIgnore
  @Column(nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role;

  @ManyToOne
  @JoinColumn(name = "church_name")
  private Church church;

  @Column(nullable = false)
  private boolean enabled = true;

  /** Default no-arg constructor required by JPA. */
  public DashboardUser() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public UserRole getRole() {
    return role;
  }

  public void setRole(UserRole role) {
    this.role = role;
  }

  public Church getChurch() {
    return church;
  }

  public void setChurch(Church church) {
    this.church = church;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public String toString() {
    return "DashboardUser{id=" + id + ", username='" + username + "', role=" + role + "}";
  }
}
