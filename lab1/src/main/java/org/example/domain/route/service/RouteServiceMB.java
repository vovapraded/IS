package org.example.domain.route.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.route.dto.*;
import org.example.domain.route.entity.Route;
import org.example.domain.route.mapper.RouteMapper;
import org.example.domain.route.repository.RouteRepositoryMB;
import org.example.domain.coordinates.service.CoordinatesServiceMB;
import org.example.domain.location.service.LocationServiceMB;
import org.example.domain.coordinates.dto.CoordinatesDto;
import org.example.domain.location.dto.LocationDto;
import org.example.domain.coordinates.repository.CoordinatesRepositoryMB;
import org.example.domain.location.repository.LocationRepositoryMB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Stateless
public class RouteServiceMB {

    @Inject
    private RouteRepositoryMB routeRepository;

    @Inject
    private CoordinatesServiceMB coordinatesService;

    @Inject
    private LocationServiceMB locationService;

    @Inject
    private CoordinatesRepositoryMB coordinatesRepository;

    @Inject
    private LocationRepositoryMB locationRepository;

    public RouteDto createRoute(RouteCreateDto dto) {
        log.info("Creating route {}", dto);
        
        // Получаем или создаем координаты
        CoordinatesDto coordsDto = getOrCreateCoordinates(dto.coordinates());
        
        // Получаем или создаем локации
        LocationDto fromDto = getOrCreateLocation(dto.from());
        LocationDto toDto = getOrCreateLocation(dto.to());
        
        // Создаем маршрут с ссылками на существующие сущности
        Route entity = new Route();
        entity.setName(dto.name());
        entity.setDistance(dto.distance());
        entity.setRating(dto.rating());
        
        // Получаем Entity объекты из репозиториев
        entity.setCoordinates(coordinatesRepository.findById(coordsDto.id()));
        entity.setFrom(locationRepository.findById(fromDto.id()));
        entity.setTo(locationRepository.findById(toDto.id()));
        
        Route saved = routeRepository.save(entity);
        return RouteMapper.toDto(saved);
    }

    public RouteDto findById(Integer id) {
        Route route = routeRepository.findById(id);
        if (route == null) {
            throw new IllegalArgumentException("Route not found with id: " + id);
        }
        return RouteMapper.toDto(route);
    }

    public List<RouteDto> findAll() {
        return routeRepository.findAll().stream()
                .map(RouteMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<RouteDto> findAll(int page, int size, String nameFilter, String sortBy, String sortDirection) {
        int offset = page * size;
        return routeRepository.findAllWithFilter(offset, size, nameFilter, sortBy, sortDirection).stream()
                .map(RouteMapper::toDto)
                .collect(Collectors.toList());
    }

    public long countAll() {
        return routeRepository.countAll();
    }

    public long countWithFilter(String nameFilter) {
        return routeRepository.countWithFilter(nameFilter);
    }

    public RouteDto updateRoute(RouteUpdateDto dto) {
        log.info("Updating route {}", dto);
        try {
            Route updated = routeRepository.updateFromDto(dto);
            return RouteMapper.toDto(updated);
        } catch (IllegalArgumentException e) {
            log.error("Failed to update route: {}", e.getMessage());
            throw e;
        }
    }

    public Map<String, Object> checkDependencies(Integer id) {
        log.info("Checking dependencies for route with id {}", id);
        
        Route routeToDelete = routeRepository.findById(id);
        if (routeToDelete == null) {
            throw new IllegalArgumentException("Route not found with id: " + id);
        }

        // Проверяем использование координат и локаций по ID, ИСКЛЮЧАЯ текущий маршрут
        long coordinatesUsageCount = coordinatesService.getUsageCountExcluding(routeToDelete.getCoordinates().getId(), id);
        long fromLocationUsageCount = locationService.getUsageCountExcluding(routeToDelete.getFrom().getId(), id);
        long toLocationUsageCount = locationService.getUsageCountExcluding(routeToDelete.getTo().getId(), id);

        boolean hasSharedResources = coordinatesUsageCount > 0 || fromLocationUsageCount > 0 || toLocationUsageCount > 0;
        
        Map<String, Object> result = new HashMap<>();
        result.put("hasSharedResources", hasSharedResources);
        result.put("coordinatesUsageCount", coordinatesUsageCount);
        result.put("fromLocationUsageCount", fromLocationUsageCount);
        result.put("toLocationUsageCount", toLocationUsageCount);
        result.put("route", RouteMapper.toDto(routeToDelete));
        
        if (hasSharedResources) {
            // Получаем список других маршрутов для возможной перепривязки
            List<RouteDto> alternativeRoutes = routeRepository.findAll().stream()
                .filter(r -> !r.getId().equals(id))
                .map(RouteMapper::toDto)
                .collect(Collectors.toList());
            result.put("alternativeRoutes", alternativeRoutes);
        }

        log.info("Route {} dependencies - shared resources: {}, coordinates: {}, from: {}, to: {}",
            id, hasSharedResources, coordinatesUsageCount, fromLocationUsageCount, toLocationUsageCount);

        return result;
    }

    public void delete(Integer id) {
        log.info("Deleting route with id {}", id);
        
        Route routeToDelete = routeRepository.findById(id);
        if (routeToDelete == null) {
            throw new IllegalArgumentException("Route not found with id: " + id);
        }

        // Запоминаем ID связанных объектов для возможной очистки
        Integer coordinatesId = routeToDelete.getCoordinates().getId();
        Integer fromLocationId = routeToDelete.getFrom().getId();
        Integer toLocationId = routeToDelete.getTo().getId();

        // Удаляем маршрут
        routeRepository.deleteById(id);

        // Проверяем и удаляем координаты/локации, если они больше не используются
        cleanupUnusedCoordinates(coordinatesId);
        cleanupUnusedLocation(fromLocationId);
        cleanupUnusedLocation(toLocationId);

        log.info("Route with id {} successfully deleted", id);
    }

    public void deleteWithRebinding(Integer id, Integer newRouteId) {
        log.info("Deleting route with id {} and rebinding related objects to route {}", id, newRouteId);
        
        Route routeToDelete = routeRepository.findById(id);
        if (routeToDelete == null) {
            throw new IllegalArgumentException("Route not found with id: " + id);
        }

        Route targetRoute = routeRepository.findById(newRouteId);
        if (targetRoute == null) {
            throw new IllegalArgumentException("Target route not found with id: " + newRouteId);
        }

        // Теперь это реальная перепривязка!
        // Переносим связанные объекты от удаляемого маршрута к целевому
        Integer oldCoordinatesId = routeToDelete.getCoordinates().getId();
        Integer oldFromLocationId = routeToDelete.getFrom().getId();
        Integer oldToLocationId = routeToDelete.getTo().getId();

        // Обновляем целевой маршрут, используя координаты и локации удаляемого маршрута
        targetRoute.setCoordinates(routeToDelete.getCoordinates());
        targetRoute.setFrom(routeToDelete.getFrom());
        targetRoute.setTo(routeToDelete.getTo());
        routeRepository.save(targetRoute);

        // Удаляем исходный маршрут
        routeRepository.deleteById(id);

        log.info("Route with id {} successfully deleted with rebinding to route {}", id, newRouteId);
    }

    private void cleanupUnusedCoordinates(Integer coordinatesId) {
        if (coordinatesService.canDelete(coordinatesId)) {
            coordinatesService.delete(coordinatesId);
            log.info("Cleaned up unused coordinates with id {}", coordinatesId);
        }
    }

    private void cleanupUnusedLocation(Integer locationId) {
        if (locationService.canDelete(locationId)) {
            locationService.delete(locationId);
            log.info("Cleaned up unused location with id {}", locationId);
        }
    }

    // Специальные операции согласно ТЗ

    public RouteDto findRouteWithMaxName() {
        log.info("Finding route with maximum name");
        Route route = routeRepository.findRouteWithMaxName();
        return route != null ? RouteMapper.toDto(route) : null;
    }

    public long countRoutesWithRatingLessThan(Long ratingThreshold) {
        log.info("Counting routes with rating less than {}", ratingThreshold);
        return routeRepository.countRoutesWithRatingLessThan(ratingThreshold);
    }

    public List<RouteDto> findRoutesWithRatingGreaterThan(Long ratingThreshold) {
        log.info("Finding routes with rating greater than {}", ratingThreshold);
        return routeRepository.findRoutesWithRatingGreaterThan(ratingThreshold).stream()
                .map(RouteMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<RouteDto> findRoutesBetweenLocations(String fromLocationName, String toLocationName, String sortBy) {
        log.info("Finding routes between {} and {} sorted by {}", fromLocationName, toLocationName, sortBy);
        return routeRepository.findRoutesBetweenLocations(fromLocationName, toLocationName, sortBy).stream()
                .map(RouteMapper::toDto)
                .collect(Collectors.toList());
    }

    public RouteDto addRouteBetweenLocations(String routeName, float coordX, Double coordY,
                                           Double fromX, double fromY, String fromName,
                                           Double toX, double toY, String toName,
                                           Long distance, Long rating) {
        log.info("Adding route between locations {} and {}", fromName, toName);
        Integer newRouteId = routeRepository.addRouteBetweenLocations(
                routeName, coordX, coordY, fromX, fromY, fromName,
                toX, toY, toName, distance, rating);
        
        Route createdRoute = routeRepository.findById(newRouteId);
        return createdRoute != null ? RouteMapper.toDto(createdRoute) : null;
    }

    // Новые методы для работы с связанными объектами

    public List<CoordinatesDto> getAvailableCoordinates() {
        return coordinatesService.findAll();
    }

    public List<LocationDto> getAvailableLocations() {
        return locationService.findAll();
    }

    public List<LocationDto> getAvailableFromLocations() {
        return locationService.findAllFrom();
    }

    public List<LocationDto> getAvailableToLocations() {
        return locationService.findAllTo();
    }

    public List<String> getAvailableLocationNames() {
        return locationService.findDistinctNames();
    }

    public CoordinatesDto getOrCreateCoordinates(CoordinatesDto coordinatesDto) {
        return coordinatesService.findOrCreate(coordinatesDto);
    }

    public LocationDto getOrCreateLocation(LocationDto locationDto) {
        return locationService.findOrCreate(locationDto);
    }
}