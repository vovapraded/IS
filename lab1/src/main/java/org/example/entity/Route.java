package org.example.entity;

import java.time.ZonedDateTime;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

@Entity
@Table(name = "routes")
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
    @Embedded
    private Coordinates coordinates;

    @NotNull
    @Column(name = "creation_date", nullable = false, updatable = false)
    private ZonedDateTime creationDate;

    @NotNull
    @Valid
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "x", column = @Column(name = "from_x", nullable = false)),
            @AttributeOverride(name = "y", column = @Column(name = "from_y", nullable = false)),
            @AttributeOverride(name = "name", column = @Column(name = "from_name"))
    })
    private Location from;

    @NotNull
    @Valid
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "x", column = @Column(name = "to_x", nullable = false)),
            @AttributeOverride(name = "y", column = @Column(name = "to_y", nullable = false)),
            @AttributeOverride(name = "name", column = @Column(name = "to_name"))
    })
    private Location to;


    @NotNull
    @Min(2)
    @Column(nullable = false)
    private Long distance;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Long rating;

    @PrePersist
    protected void onCreate() {
        this.creationDate = ZonedDateTime.now();
    }
}
