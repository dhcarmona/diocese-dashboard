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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@NoArgsConstructor
@ToString
@Getter
@Setter
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
}
