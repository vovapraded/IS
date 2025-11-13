package org.example.dto;

public record RouteCreateDto(
        String name,
        CoordinatesDto coordinates,
        LocationDto from,
        LocationDto to,
        Long distance,
        Long rating
) {}
