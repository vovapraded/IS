package org.example.domain.location.mapper;

import org.example.domain.location.dto.LocationDto;
import org.example.domain.location.entity.Location;

public class LocationMapper {

    public static LocationDto toDto(Location entity) {
        if (entity == null) {
            return null;
        }
        Integer ownerRouteId = null;
        String ownerRouteName = null;
        
        if (entity.getOwnerRoute() != null) {
            ownerRouteId = entity.getOwnerRoute().getId();
            ownerRouteName = entity.getOwnerRoute().getName();
        }
        
        return new LocationDto(
                entity.getId(),
                entity.getX(),
                entity.getY(),
                entity.getName(),
                ownerRouteId,
                ownerRouteName
        );
    }

    public static Location toEntity(LocationDto dto) {
        if (dto == null) {
            return null;
        }
        return Location.builder()
                .id(dto.id())
                .x(dto.x())
                .y(dto.y())
                .name(dto.name())
                // ownerRoute будет установлен в сервисе
                .build();
    }
}