package org.example.domain.route.entity;

import java.time.ZonedDateTime;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FetchType;
import jakarta.persistence.FieldResult;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.example.domain.coordinates.entity.Coordinates;
import org.example.domain.location.entity.Location;

@Entity
@Table(name = "routes")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SqlResultSetMappings({
    @SqlResultSetMapping(
        name = "RouteMapping",
        entities = @EntityResult(
            entityClass = Route.class,
            fields = {
                @FieldResult(name = "id", column = "id"),
                @FieldResult(name = "name", column = "name"),
                @FieldResult(name = "coordinates.x", column = "x"),
                @FieldResult(name = "coordinates.y", column = "y"),
                @FieldResult(name = "from.x", column = "from_x"),
                @FieldResult(name = "from.y", column = "from_y"),
                @FieldResult(name = "from.name", column = "from_name"),
                @FieldResult(name = "to.x", column = "to_x"),
                @FieldResult(name = "to.y", column = "to_y"),
                @FieldResult(name = "to.name", column = "to_name"),
                @FieldResult(name = "distance", column = "distance"),
                @FieldResult(name = "rating", column = "rating"),
                @FieldResult(name = "creationDate", column = "creation_date")
            }
        )
    )
})
@Getter
@Setter
@NoArgsConstructor
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @Valid
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "coordinates_id", nullable = false)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Coordinates coordinates;

    @NotNull
    @Column(name = "creation_date", nullable = false, updatable = false)
    private ZonedDateTime creationDate;

    @NotNull
    @Valid
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "from_location_id", nullable = false)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Location from;

    @NotNull
    @Valid
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "to_location_id", nullable = false)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Location to;


    @NotNull
    @Min(2)
    @Column(nullable = false)
    private Long distance;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Long rating;

    @jakarta.persistence.Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.creationDate = ZonedDateTime.now();
    }
}