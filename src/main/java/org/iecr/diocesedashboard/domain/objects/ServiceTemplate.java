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
}
