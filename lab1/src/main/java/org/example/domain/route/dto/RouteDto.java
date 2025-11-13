package org.example.domain.route.dto;

import java.time.ZonedDateTime;
import org.example.domain.coordinates.dto.CoordinatesDto;
import org.example.domain.location.dto.LocationDto;

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