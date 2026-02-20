package org.iecr.diocesedashboard.domain.objects;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.util.Set;

@Entity
public class Church {

  @Id
  private String name;

  private String location;

  @OneToOne
  @JoinColumn(name = "main_celebrant_id")
  private Celebrant mainCelebrant;

  @OneToMany(mappedBy = "church")
  private Set<ServiceInstance> services;

  public Church() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public Celebrant getMainCelebrant() {
    return mainCelebrant;
  }

  public void setMainCelebrant(Celebrant mainCelebrant) {
    this.mainCelebrant = mainCelebrant;
  }

  public Set<ServiceInstance> getServices() {
    return services;
  }

  public void setServices(Set<ServiceInstance> services) {
    this.services = services;
  }

  @Override
  public String toString() {
    return "Church{name='" + name + "', location='" + location + "'}";
  }
}
