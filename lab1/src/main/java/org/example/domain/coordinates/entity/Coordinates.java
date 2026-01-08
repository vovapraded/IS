package org.example.domain.coordinates.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.example.domain.route.entity.Route;

@Entity
@Table(name = "coordinates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Coordinates {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(nullable = false)
    private float x;

    @NotNull
    @Max(807)
    @Column(nullable = false)
    private Double y;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_route_id", nullable = true)
    private Route ownerRoute; // Маршрут-владелец этих координат

    @jakarta.persistence.Version
    @Column(name = "version", nullable = false)
    private Long version;
}