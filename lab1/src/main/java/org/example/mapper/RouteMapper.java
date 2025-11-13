package org.example.mapper;

import org.example.dto.*;
import org.example.entity.Route;

public class RouteMapper {

    public static RouteDto toDto(Route entity) {
        if (entity == null) {
            return null;
        }
        return new RouteDto(
                entity.getId(),
                entity.getName(),
                CoordinatesMapper.toDto(entity.getCoordinates()),
                entity.getCreationDate(),
                LocationMapper.toDto(entity.getFrom()),
                LocationMapper.toDto(entity.getTo()),
                entity.getDistance(),
                entity.getRating()
        );
    }

    public static Route fromCreateDto(RouteCreateDto dto) {
        if (dto == null) {
            return null;
        }
        Route r = new Route();
        // id бдшка сгенерит
        r.setName(dto.name());
        r.setCoordinates(CoordinatesMapper.toEntity(dto.coordinates()));
        r.setFrom(LocationMapper.toEntity(dto.from()));
        r.setTo(LocationMapper.toEntity(dto.to()));
        r.setDistance(dto.distance());
        r.setRating(dto.rating());
        // creationDate будет установлена в @PrePersist
        return r;
    }

    public static Route fromUpdateDto(RouteUpdateDto dto) {
        if (dto == null) {
            return null;
        }
        Route r = new Route();
        r.setId(dto.id()); // при апдейте id обязателен
        r.setName(dto.name());
        r.setCoordinates(CoordinatesMapper.toEntity(dto.coordinates()));
        r.setFrom(LocationMapper.toEntity(dto.from()));
        r.setTo(LocationMapper.toEntity(dto.to()));
        r.setDistance(dto.distance());
        r.setRating(dto.rating());
        return r;
    }
    public static void updateEntityFromDto(Route entity, RouteUpdateDto dto) {
        if (entity == null || dto == null) {
            throw new IllegalArgumentException("Entity and DTO cannot be null");
        }
        
        entity.setName(dto.name());
        entity.setCoordinates(CoordinatesMapper.toEntity(dto.coordinates()));
        entity.setFrom(LocationMapper.toEntity(dto.from()));
        entity.setTo(LocationMapper.toEntity(dto.to()));
        entity.setDistance(dto.distance());
        entity.setRating(dto.rating());
        // ID и creationDate не изменяем при обновлении
    }

}
