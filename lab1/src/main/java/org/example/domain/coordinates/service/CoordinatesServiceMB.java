package org.example.domain.coordinates.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    @PersistenceContext(unitName = "RoutesPU")
    private EntityManager em;

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
        // Загружаем coordinates через наш EntityManager
        Coordinates coordinates = em.find(Coordinates.class, coordinatesId);
        if (coordinates != null) {
            // Если newOwner из другого контекста, приводим к нашему
            Route managedOwner = null;
            if (newOwner != null) {
                if (em.contains(newOwner)) {
                    managedOwner = newOwner;
                } else {
                    managedOwner = em.merge(newOwner);
                }
            }
            
            coordinates.setOwnerRoute(managedOwner);
            em.merge(coordinates);
        }
    }

    public void transferOwnershipById(Integer coordinatesId, Integer newOwnerId) {
        // Загружаем coordinates через наш EntityManager для единого контекста
        Coordinates coordinates = em.find(Coordinates.class, coordinatesId);
        if (coordinates != null) {
            Route newOwner = null;
            if (newOwnerId != null) {
                // Используем find для получения полноценного managed объекта
                // Это решает проблему с TransientObjectException
                newOwner = em.find(Route.class, newOwnerId);
                if (newOwner == null) {
                    throw new IllegalArgumentException("Route not found with id: " + newOwnerId);
                }
            }
            
            // Оба объекта теперь из одного EntityManager контекста
            coordinates.setOwnerRoute(newOwner);
            // Сохраняем через EntityManager для консистентности
            em.merge(coordinates);
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