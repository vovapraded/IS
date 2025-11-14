package org.example.domain.coordinates.mapper;

import org.example.domain.coordinates.dto.CoordinatesDto;
import org.example.domain.coordinates.entity.Coordinates;

public class CoordinatesMapper {

    public static CoordinatesDto toDto(Coordinates entity) {
        if (entity == null) {
            return null;
        }
        Integer ownerRouteId = null;
        String ownerRouteName = null;
        
        if (entity.getOwnerRoute() != null) {
            ownerRouteId = entity.getOwnerRoute().getId();
            ownerRouteName = entity.getOwnerRoute().getName();
        }
        
        return new CoordinatesDto(
                entity.getId(),
                entity.getX(),
                entity.getY(),
                ownerRouteId,
                ownerRouteName
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
                // ownerRoute будет установлен в сервисе
                .build();
    }
}