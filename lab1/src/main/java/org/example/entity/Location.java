package org.example.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

    @NotNull
    @Column(nullable = false)
    private Double x;

    @Column(nullable = false)
    private double y;

    private String name;
}
