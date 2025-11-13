package org.example.mapper;

import org.example.dto.CoordinatesDto;
import org.example.entity.Coordinates;

public class CoordinatesMapper {

    public static CoordinatesDto toDto(Coordinates entity) {
        if (entity == null) {
            return null;
        }
        return new CoordinatesDto(
                entity.getX(),
                entity.getY()
        );
    }

    public static Coordinates toEntity(CoordinatesDto dto) {
        if (dto == null) {
            return null;
        }
        return Coordinates.builder()
                .x(dto.x())
                .y(dto.y())
                .build();
    }
}
