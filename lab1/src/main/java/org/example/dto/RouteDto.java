package org.example.dto;

import java.time.ZonedDateTime;

public record RouteDto(
        Integer id,
        String name,
        CoordinatesDto coordinates,
        ZonedDateTime creationDate,
        LocationDto from,
        LocationDto to,
        Long distance,
        Long rating
) {}
