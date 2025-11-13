package org.example.mapper;

import org.example.dto.LocationDto;
import org.example.entity.Location;

public class LocationMapper {

    public static LocationDto toDto(Location entity) {
        if (entity == null) {
            return null;
        }
        return new LocationDto(
                entity.getX(),
                entity.getY(),
                entity.getName()
        );
    }

    public static Location toEntity(LocationDto dto) {
        if (dto == null) {
            return null;
        }
        return Location.builder()
                .x(dto.x())
                .y(dto.y())
                .name(dto.name())
                .build();
    }
}
