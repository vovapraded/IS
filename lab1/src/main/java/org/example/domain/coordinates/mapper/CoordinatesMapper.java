package org.example.domain.coordinates.mapper;

import org.example.domain.coordinates.dto.CoordinatesDto;
import org.example.domain.coordinates.entity.Coordinates;

public class CoordinatesMapper {

    public static CoordinatesDto toDto(Coordinates entity) {
        if (entity == null) {
            return null;
        }
        return new CoordinatesDto(
                entity.getId(),
                entity.getX(),
                entity.getY()
        );
    }

    public static Coordinates toEntity(CoordinatesDto dto) {
        if (dto == null) {
            return null;
        }
        return Coordinates.builder()
                .id(dto.id())
                .x(dto.x())
                .y(dto.y())
                .build();
    }
}