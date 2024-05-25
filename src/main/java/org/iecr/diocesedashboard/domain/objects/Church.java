package org.iecr.diocesedashboard.domain.objects;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@NoArgsConstructor
@ToString
@Getter
@Setter
public class Church {

  @Id
  private String name;

  private String location;

  @OneToOne
  @JoinColumn(name = "main_celebrant_id")
  private Celebrant mainCelebrant;

  @OneToMany(mappedBy = "church")
  private Set<ServiceInstance> services;
}
