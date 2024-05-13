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
public class Church {

    @Id
    private String name;

    private String location;

    @OneToOne
    @JoinColumn(name="main_celebrant_id")
    private Celebrant mainCelebrant;

    @OneToMany(mappedBy = "church")
    private Set<ServiceInstance> services;
}
