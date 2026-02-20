package org.iecr.diocesedashboard.domain.objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.Set;

@Entity
public class ServiceTemplate {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(unique = true, nullable = false)
  private Long id;

  @Column(unique = true, nullable = false)
  private String serviceTemplateName;

  @OneToMany(mappedBy = "serviceTemplate")
  private Set<ServiceInstance> serviceInstances;

  @OneToMany(mappedBy = "serviceTemplate")
  private Set<ServiceInfoItem> serviceInfoItems;

  public ServiceTemplate() {}

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getServiceTemplateName() { return serviceTemplateName; }
  public void setServiceTemplateName(String serviceTemplateName) { this.serviceTemplateName = serviceTemplateName; }

  public Set<ServiceInstance> getServiceInstances() { return serviceInstances; }
  public void setServiceInstances(Set<ServiceInstance> serviceInstances) { this.serviceInstances = serviceInstances; }

  public Set<ServiceInfoItem> getServiceInfoItems() { return serviceInfoItems; }
  public void setServiceInfoItems(Set<ServiceInfoItem> serviceInfoItems) { this.serviceInfoItems = serviceInfoItems; }

  @Override
  public String toString() {
    return "ServiceTemplate{id=" + id + ", serviceTemplateName='" + serviceTemplateName + "'}";
  }
}

