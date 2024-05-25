package org.iecr.diocesedashboard.domain.objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@NoArgsConstructor
@ToString
@Getter
@Setter
public class ServiceInfoItem {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(unique = true, nullable = false)
  private Long id;

  @Column(unique = true, nullable = false)
  private String questionId;

  @ManyToOne
  @JoinColumn(nullable = false)
  private ServiceTemplate serviceTemplate;

  private Boolean required;

  @Column(nullable = false)
  private ServiceInfoItemType serviceInfoItemType;
}
