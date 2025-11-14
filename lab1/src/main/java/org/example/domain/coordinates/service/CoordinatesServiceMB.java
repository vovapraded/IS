package org.example.domain.coordinates.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.domain.coordinates.dto.CoordinatesDto;
import org.example.domain.coordinates.entity.Coordinates;
import org.example.domain.coordinates.mapper.CoordinatesMapper;
import org.example.domain.coordinates.repository.CoordinatesRepositoryMB;
import org.example.domain.route.entity.Route;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Stateless
public class CoordinatesServiceMB {

    @Inject
    private CoordinatesRepositoryMB coordinatesRepository;

    public CoordinatesDto findById(Integer id) {
        Coordinates coordinates = coordinatesRepository.findById(id);
        return coordinates != null ? CoordinatesMapper.toDto(coordinates) : null;
    }

    public List<CoordinatesDto> findAll() {
        return coordinatesRepository.findAll().stream()
                .map(CoordinatesMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<CoordinatesDto> findByExample(float x, Double y) {
        return coordinatesRepository.findByExample(x, y).stream()
                .map(CoordinatesMapper::toDto)
                .collect(Collectors.toList());
    }

    public boolean exists(float x, Double y) {
        return coordinatesRepository.exists(x, y);
    }

    public CoordinatesDto findOrCreate(CoordinatesDto dto) {
        // Проверяем, существуют ли уже такие координаты
        Optional<Coordinates> existing = coordinatesRepository.findByXAndY(dto.x(), dto.y());
        if (existing.isPresent()) {
            return CoordinatesMapper.toDto(existing.get());
        }
        
        // Создаем новые координаты
        Coordinates newCoordinates = Coordinates.builder()
                .x(dto.x())
                .y(dto.y())
                .build();
        
        Coordinates saved = coordinatesRepository.save(newCoordinates);
        return CoordinatesMapper.toDto(saved);
    }

    public CoordinatesDto findOrCreateWithOwner(CoordinatesDto dto, Route ownerRoute) {
        // Проверяем, существуют ли уже такие координаты
        Optional<Coordinates> existing = coordinatesRepository.findByXAndY(dto.x(), dto.y());
        if (existing.isPresent()) {
            return CoordinatesMapper.toDto(existing.get());
        }
        
        // Создаем новые координаты с владельцем
        Coordinates newCoordinates = Coordinates.builder()
                .x(dto.x())
                .y(dto.y())
                .ownerRoute(ownerRoute)
                .build();
        
        Coordinates saved = coordinatesRepository.save(newCoordinates);
        return CoordinatesMapper.toDto(saved);
    }

    public void transferOwnership(Integer coordinatesId, Route newOwner) {
        Coordinates coordinates = coordinatesRepository.findById(coordinatesId);
        if (coordinates != null) {
            coordinates.setOwnerRoute(newOwner);
            coordinatesRepository.save(coordinates);
        }
    }
    
    public void updateOwner(Integer coordinatesId, Route ownerRoute) {
        transferOwnership(coordinatesId, ownerRoute);
    }

    public long getUsageCount(Integer coordinatesId) {
        return coordinatesRepository.countUsages(coordinatesId);
    }
    
    public long getUsageCountExcluding(Integer coordinatesId, Integer excludeRouteId) {
        return coordinatesRepository.countUsagesExcluding(coordinatesId, excludeRouteId);
    }

    public long getUsageCount(float x, Double y) {
        return coordinatesRepository.countUsagesByValues(x, y);
    }

    public void delete(Integer id) {
        coordinatesRepository.deleteById(id);
    }

    public boolean canDelete(Integer id) {
        return getUsageCount(id) == 0;
    }
}