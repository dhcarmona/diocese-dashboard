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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Transient;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Set;

@Entity
public class ServiceTemplate {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(unique = true, nullable = false)
  private Long id;

  @Column(unique = true, nullable = false)
  private String serviceTemplateName;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  private ServiceTemplateType templateType;

  @Transient
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private String bannerUrl;

  @JsonIgnore
  @OneToMany(mappedBy = "serviceTemplate")
  private Set<ServiceInstance> serviceInstances;

  @OneToMany(mappedBy = "serviceTemplate")
  @OrderBy("sortOrder ASC")
  private List<ServiceInfoItem> serviceInfoItems;

  public ServiceTemplate() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getServiceTemplateName() {
    return serviceTemplateName;
  }

  public void setServiceTemplateName(String serviceTemplateName) {
    this.serviceTemplateName = serviceTemplateName;
  }

  public ServiceTemplateType getTemplateType() {
    return templateType;
  }

  public void setTemplateType(ServiceTemplateType templateType) {
    this.templateType = templateType;
  }

  public String getBannerUrl() {
    return bannerUrl;
  }

  public void setBannerUrl(String bannerUrl) {
    this.bannerUrl = bannerUrl;
  }

  public Set<ServiceInstance> getServiceInstances() {
    return serviceInstances;
  }

  public void setServiceInstances(Set<ServiceInstance> serviceInstances) {
    this.serviceInstances = serviceInstances;
  }

  public List<ServiceInfoItem> getServiceInfoItems() {
    return serviceInfoItems;
  }

  public void setServiceInfoItems(List<ServiceInfoItem> serviceInfoItems) {
    this.serviceInfoItems = serviceInfoItems;
  }

  @Override
  public String toString() {
    return "ServiceTemplate{id=" + id + ", serviceTemplateName='" + serviceTemplateName + "'}";
  }
}
