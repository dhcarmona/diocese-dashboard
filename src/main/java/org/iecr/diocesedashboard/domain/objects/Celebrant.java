package org.iecr.diocesedashboard.domain.objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import java.util.Set;

@Entity
public class Celebrant {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(unique = true, nullable = false)
  private Long id;

  @Column(nullable = false)
  private String name;

  @OneToOne(mappedBy = "mainCelebrant")
  private Church mainChurch;

  @ManyToMany
  @JoinTable(name = "celebrant_service")
  private Set<ServiceInstance> servicesCelebrated;

  public Celebrant() {}

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public Church getMainChurch() { return mainChurch; }
  public void setMainChurch(Church mainChurch) { this.mainChurch = mainChurch; }

  public Set<ServiceInstance> getServicesCelebrated() { return servicesCelebrated; }
  public void setServicesCelebrated(Set<ServiceInstance> servicesCelebrated) { this.servicesCelebrated = servicesCelebrated; }

  @Override
  public String toString() {
    return "Celebrant{id=" + id + ", name='" + name + "'}";
  }
}

