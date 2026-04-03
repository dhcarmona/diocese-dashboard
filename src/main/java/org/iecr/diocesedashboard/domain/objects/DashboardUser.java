package org.iecr.diocesedashboard.domain.objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

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
  @Column(nullable = true)
  private String passwordHash;

  @Column
  private String fullName;

  @Column(length = 50)
  private String phoneNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "dashboard_user_church",
      joinColumns = @JoinColumn(name = "dashboard_user_id"),
      inverseJoinColumns = @JoinColumn(name = "church_name"))
  private Set<Church> assignedChurches = new HashSet<>();

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

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public UserRole getRole() {
    return role;
  }

  public void setRole(UserRole role) {
    this.role = role;
  }

  public Set<Church> getAssignedChurches() {
    return assignedChurches;
  }

  public void setAssignedChurches(Set<Church> assignedChurches) {
    this.assignedChurches = assignedChurches == null
        ? new HashSet<>()
        : new HashSet<>(assignedChurches);
  }

  public boolean isAssignedToChurch(Church church) {
    return church != null && isAssignedToChurchName(church.getName());
  }

  public boolean isAssignedToChurchName(String churchName) {
    return churchName != null
        && assignedChurches.stream()
        .map(Church::getName)
        .anyMatch(churchName::equals);
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
