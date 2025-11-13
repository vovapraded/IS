package org.example.domain.route.dto;

import org.example.domain.coordinates.dto.CoordinatesDto;
import org.example.domain.location.dto.LocationDto;

public record RouteCreateDto(
        String name,
        CoordinatesDto coordinates,
        LocationDto from,
        LocationDto to,
        Long distance,
        Long rating
) {}