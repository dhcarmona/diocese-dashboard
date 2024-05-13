package org.iecr.diocesedashboard.domain.objects;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.Set;

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
    @JoinColumn(name="church_id", nullable = false)
    private Church church;

    @ManyToOne
    @JoinColumn(name = "template_id", nullable = false)
    private ServiceTemplate serviceTemplate;

    @ManyToMany(mappedBy = "servicesCelebrated")
    private Set<Celebrant> celebrants;
}
