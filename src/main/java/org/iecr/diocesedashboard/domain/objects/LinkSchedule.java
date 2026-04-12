package org.iecr.diocesedashboard.domain.objects;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a recurring schedule that automatically creates and sends reporter links
 * for a set of churches at a given hour (Costa Rica time, UTC-6) on specific days of the week.
 *
 * <p>Each trigger creates a fresh batch of links identical to manual bulk creation,
 * including WhatsApp notifications to reporters.
 */
@Entity
@Table(name = "link_schedule")
public class LinkSchedule {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "link_schedule_seq")
  @SequenceGenerator(name = "link_schedule_seq", allocationSize = 50)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "service_template_id", nullable = false)
  private ServiceTemplate serviceTemplate;

  /**
   * The days of the week on which this schedule fires.
   * Stored as the {@link DayOfWeek} name (e.g. {@code "MONDAY"}).
   */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "link_schedule_day",
      joinColumns = @JoinColumn(name = "schedule_id"))
  @Column(name = "day_of_week", nullable = false)
  @Enumerated(EnumType.STRING)
  private Set<DayOfWeek> daysOfWeek = new HashSet<>();

  /**
   * The church names for which links will be created when this schedule fires.
   */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "link_schedule_church",
      joinColumns = @JoinColumn(name = "schedule_id"))
  @Column(name = "church_name", nullable = false)
  private Set<String> churchNames = new HashSet<>();

  /**
   * The hour of day (0–23) in Costa Rica local time (America/Costa_Rica, UTC-6)
   * at which this schedule should fire.
   */
  @Column(name = "send_hour", nullable = false)
  private int sendHour;

  /** The date on which this schedule was last successfully triggered. Null if never triggered. */
  @Column(name = "last_triggered_date")
  private LocalDate lastTriggeredDate;

  /** The timestamp when this schedule was created. */
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** Default no-arg constructor required by JPA. */
  public LinkSchedule() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ServiceTemplate getServiceTemplate() {
    return serviceTemplate;
  }

  public void setServiceTemplate(ServiceTemplate serviceTemplate) {
    this.serviceTemplate = serviceTemplate;
  }

  public Set<DayOfWeek> getDaysOfWeek() {
    return daysOfWeek;
  }

  public void setDaysOfWeek(Set<DayOfWeek> daysOfWeek) {
    this.daysOfWeek = daysOfWeek;
  }

  public Set<String> getChurchNames() {
    return churchNames;
  }

  public void setChurchNames(Set<String> churchNames) {
    this.churchNames = churchNames;
  }

  public int getSendHour() {
    return sendHour;
  }

  public void setSendHour(int sendHour) {
    this.sendHour = sendHour;
  }

  public LocalDate getLastTriggeredDate() {
    return lastTriggeredDate;
  }

  public void setLastTriggeredDate(LocalDate lastTriggeredDate) {
    this.lastTriggeredDate = lastTriggeredDate;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public String toString() {
    return "LinkSchedule{id=" + id + ", sendHour=" + sendHour
        + ", daysOfWeek=" + daysOfWeek + "}";
  }
}
