package org.iecr.diocesedashboard.domain.objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;

/** A visual section header within a service template, used to group related info items. */
@Entity
public class SectionHeader {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(unique = true, nullable = false)
  private Long id;

  @NotBlank
  @Column(nullable = false)
  private String title;

  @JsonIgnore
  @ManyToOne
  @JoinColumn(nullable = false)
  private ServiceTemplate serviceTemplate;

  @Column(name = "sort_order")
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Integer sortOrder;

  public SectionHeader() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public ServiceTemplate getServiceTemplate() {
    return serviceTemplate;
  }

  public void setServiceTemplate(ServiceTemplate serviceTemplate) {
    this.serviceTemplate = serviceTemplate;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  @Override
  public String toString() {
    return "SectionHeader{id=" + id + ", title='" + title + "'}";
  }
}
