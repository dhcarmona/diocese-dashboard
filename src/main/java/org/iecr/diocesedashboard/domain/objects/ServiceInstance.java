package org.iecr.diocesedashboard.domain.objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

import java.util.Set;

@Entity
public class ServiceInstance {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(unique = true, nullable = false)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "church_id", nullable = false)
  private Church church;

  @ManyToOne
  @JoinColumn(name = "template_id", nullable = false)
  private ServiceTemplate serviceTemplate;

  @ManyToMany(mappedBy = "servicesCelebrated")
  private Set<Celebrant> celebrants;

  public ServiceInstance() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public Set<Celebrant> getCelebrants() {
    return celebrants;
  }

  public void setCelebrants(Set<Celebrant> celebrants) {
    this.celebrants = celebrants;
  }

  @Override
  public String toString() {
    return "ServiceInstance{id=" + id + "}";
  }
}
