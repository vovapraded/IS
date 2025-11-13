package org.example.dto;

public record RouteUpdateDto(
        Integer id,
        String name,
        CoordinatesDto coordinates,
        LocationDto from,
        LocationDto to,
        Long distance,
        Long rating
) {}
