package org.iecr.diocesedashboard.domain.objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;

import java.time.LocalDate;

/**
 * A short-URL token that links a specific REPORTER user to a specific ServiceTemplate,
 * allowing them to submit a service report without knowing the template ID.
 * Tokens are created by ADMIN users and distributed to reporters.
 */
@Entity
public class ReporterLink {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reporter_link_seq")
  @SequenceGenerator(name = "reporter_link_seq", allocationSize = 50)
  private Long id;

  @Column(unique = true, nullable = false)
  private String token;

  @ManyToOne
  @JoinColumn(name = "reporter_id", nullable = false)
  private DashboardUser reporter;

  @ManyToOne
  @JoinColumn(name = "church_name", nullable = false)
  private Church church;

  @ManyToOne
  @JoinColumn(name = "service_template_id", nullable = false)
  private ServiceTemplate serviceTemplate;

  @Column(name = "active_date", nullable = false)
  private LocalDate activeDate;

  /** Default no-arg constructor required by JPA. */
  public ReporterLink() {
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

  public DashboardUser getReporter() {
    return reporter;
  }

  public void setReporter(DashboardUser reporter) {
    this.reporter = reporter;
  }

  public Church getChurch() {
    return church;
  }

  public void setChurch(Church church) {
    this.church = church;
  }

  public ServiceTemplate getServiceTemplate() {
    return serviceTemplate;
  }

  public void setServiceTemplate(ServiceTemplate serviceTemplate) {
    this.serviceTemplate = serviceTemplate;
  }

  public LocalDate getActiveDate() {
    return activeDate;
  }

  public void setActiveDate(LocalDate activeDate) {
    this.activeDate = activeDate;
  }

  private String getMaskedToken() {
    if (token == null || token.length() <= 4) {
      return "****";
    }
    return token.substring(0, 4) + "****";
  }

  @Override
  public String toString() {
    return "ReporterLink{id=" + id + ", token='" + getMaskedToken() + "'}";
  }
}
