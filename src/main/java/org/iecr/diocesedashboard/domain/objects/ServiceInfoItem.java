package org.iecr.diocesedashboard.domain.objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Set;

@Entity
public class ServiceInfoItem {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(unique = true, nullable = false)
  private Long id;

  @NotBlank
  @Column(nullable = false)
  private String title;

  @Column(length = 1000)
  private String description;

  @JsonIgnore
  @ManyToOne
  @JoinColumn(nullable = false)
  private ServiceTemplate serviceTemplate;

  private Boolean required;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  private ServiceInfoItemType serviceInfoItemType;

  @Column(name = "sort_order")
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Integer sortOrder;

  @JsonIgnore
  @OneToMany(mappedBy = "serviceInfoItem")
  private Set<ServiceInfoItemResponse> responses;

  public ServiceInfoItem() {
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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ServiceTemplate getServiceTemplate() {
    return serviceTemplate;
  }

  public void setServiceTemplate(ServiceTemplate serviceTemplate) {
    this.serviceTemplate = serviceTemplate;
  }

  public Boolean getRequired() {
    return required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  public ServiceInfoItemType getServiceInfoItemType() {
    return serviceInfoItemType;
  }

  public void setServiceInfoItemType(ServiceInfoItemType serviceInfoItemType) {
    this.serviceInfoItemType = serviceInfoItemType;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public Set<ServiceInfoItemResponse> getResponses() {
    return responses;
  }

  public void setResponses(Set<ServiceInfoItemResponse> responses) {
    this.responses = responses;
  }

  @Override
  public String toString() {
    return "ServiceInfoItem{id=" + id + ", title='" + title + "'}";
  }
}
