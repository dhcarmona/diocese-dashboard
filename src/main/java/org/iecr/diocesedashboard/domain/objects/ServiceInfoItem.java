package org.iecr.diocesedashboard.domain.objects;

import jakarta.persistence.*;
import lombok.*;

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
