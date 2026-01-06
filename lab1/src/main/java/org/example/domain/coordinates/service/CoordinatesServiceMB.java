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
        // Простой подход: сначала ищем, потом создаем
        Optional<Coordinates> existing = coordinatesRepository.findByXAndY(dto.x(), dto.y());
        if (existing.isPresent()) {
            return CoordinatesMapper.toDto(existing.get());
        }
        
        // Пытаемся создать новые координаты
        try {
            Coordinates newCoordinates = Coordinates.builder()
                    .x(dto.x())
                    .y(dto.y())
                    .build();
            
            Coordinates saved = coordinatesRepository.save(newCoordinates);
            return CoordinatesMapper.toDto(saved);
        } catch (Exception e) {
            // Если constraint violation - ищем заново (другой поток уже создал)
            if (e.getCause() != null && e.getCause().getMessage() != null &&
                e.getCause().getMessage().contains("duplicate key")) {
                
                Optional<Coordinates> retry = coordinatesRepository.findByXAndY(dto.x(), dto.y());
                if (retry.isPresent()) {
                    return CoordinatesMapper.toDto(retry.get());
                }
            }
            throw e;
        }
    }

    public CoordinatesDto findOrCreateWithOwner(CoordinatesDto dto, Route ownerRoute) {
        // Простой подход: сначала ищем, потом создаем
        Optional<Coordinates> existing = coordinatesRepository.findByXAndY(dto.x(), dto.y());
        if (existing.isPresent()) {
            return CoordinatesMapper.toDto(existing.get());
        }
        
        // Пытаемся создать новые координаты с владельцем
        try {
            Coordinates newCoordinates = Coordinates.builder()
                    .x(dto.x())
                    .y(dto.y())
                    .ownerRoute(ownerRoute)
                    .build();
            
            Coordinates saved = coordinatesRepository.save(newCoordinates);
            return CoordinatesMapper.toDto(saved);
        } catch (Exception e) {
            // Если constraint violation - ищем заново (другой поток уже создал)
            if (e.getCause() != null && e.getCause().getMessage() != null &&
                e.getCause().getMessage().contains("duplicate key")) {
                
                Optional<Coordinates> retry = coordinatesRepository.findByXAndY(dto.x(), dto.y());
                if (retry.isPresent()) {
                    return CoordinatesMapper.toDto(retry.get());
                }
            }
            throw e;
        }
    }

    public void transferOwnership(Integer coordinatesId, Route newOwner) {
        Coordinates coordinates = em.find(Coordinates.class, coordinatesId);
        if (coordinates != null) {
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
        Coordinates coordinates = em.find(Coordinates.class, coordinatesId);
        if (coordinates != null) {
            Route newOwner = null;
            if (newOwnerId != null) {
                newOwner = em.find(Route.class, newOwnerId);
                if (newOwner == null) {
                    throw new IllegalArgumentException("Route not found with id: " + newOwnerId);
                }
            }
            
            coordinates.setOwnerRoute(newOwner);
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
