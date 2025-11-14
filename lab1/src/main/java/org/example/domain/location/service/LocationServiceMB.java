package org.example.domain.location.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.domain.location.dto.LocationDto;
import org.example.domain.location.entity.Location;
import org.example.domain.location.mapper.LocationMapper;
import org.example.domain.location.repository.LocationRepositoryMB;
import org.example.domain.route.entity.Route;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Stateless
public class LocationServiceMB {

    @Inject
    private LocationRepositoryMB locationRepository;

    public LocationDto findById(Integer id) {
        Location location = locationRepository.findById(id);
        return location != null ? LocationMapper.toDto(location) : null;
    }

    public List<LocationDto> findAll() {
        return locationRepository.findAll().stream()
                .map(LocationMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<LocationDto> findAllFrom() {
        return locationRepository.findAllFrom().stream()
                .map(LocationMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<LocationDto> findAllTo() {
        return locationRepository.findAllTo().stream()
                .map(LocationMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<LocationDto> findByExample(Double x, Double y, String name) {
        return locationRepository.findByExample(x, y, name).stream()
                .map(LocationMapper::toDto)
                .collect(Collectors.toList());
    }

    public boolean exists(Double x, double y, String name) {
        return locationRepository.exists(x, y, name);
    }

    public LocationDto findOrCreate(LocationDto dto) {
        // Проверяем, существует ли уже такая локация
        Optional<Location> existing = locationRepository.findByXAndYAndName(dto.x(), dto.y(), dto.name());
        if (existing.isPresent()) {
            return LocationMapper.toDto(existing.get());
        }
        
        // Создаем новую локацию
        Location newLocation = Location.builder()
                .x(dto.x())
                .y(dto.y())
                .name(dto.name())
                .build();
        
        Location saved = locationRepository.save(newLocation);
        return LocationMapper.toDto(saved);
    }

    public LocationDto findOrCreateWithOwner(LocationDto dto, Route ownerRoute) {
        // Проверяем, существует ли уже такая локация
        Optional<Location> existing = locationRepository.findByXAndYAndName(dto.x(), dto.y(), dto.name());
        if (existing.isPresent()) {
            return LocationMapper.toDto(existing.get());
        }
        
        // Создаем новую локацию с владельцем
        Location newLocation = Location.builder()
                .x(dto.x())
                .y(dto.y())
                .name(dto.name())
                .ownerRoute(ownerRoute)
                .build();
        
        Location saved = locationRepository.save(newLocation);
        return LocationMapper.toDto(saved);
    }

    public void transferOwnership(Integer locationId, Route newOwner) {
        Location location = locationRepository.findById(locationId);
        if (location != null) {
            location.setOwnerRoute(newOwner);
            locationRepository.save(location);
        }
    }
    
    public void updateOwner(Integer locationId, Route ownerRoute) {
        transferOwnership(locationId, ownerRoute);
    }

    public long getUsageCount(Integer locationId) {
        return locationRepository.countUsages(locationId);
    }
    
    public long getUsageCountExcluding(Integer locationId, Integer excludeRouteId) {
        return locationRepository.countUsagesExcluding(locationId, excludeRouteId);
    }

    public long getUsageCount(Double x, double y, String name) {
        return locationRepository.countUsagesByValues(x, y, name);
    }

    public List<String> findDistinctNames() {
        return locationRepository.findDistinctNames();
    }

    public List<LocationDto> searchByName(String namePattern) {
        if (namePattern == null || namePattern.trim().isEmpty()) {
            return findAll();
        }
        return findByExample(null, null, namePattern);
    }

    public void delete(Integer id) {
        locationRepository.deleteById(id);
    }

    public boolean canDelete(Integer id) {
        return getUsageCount(id) == 0;
    }
}