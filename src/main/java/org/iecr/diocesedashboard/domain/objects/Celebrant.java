package org.iecr.diocesedashboard.domain.objects;

import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

@Entity
@NoArgsConstructor
@ToString
@Getter
@Setter
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
}
