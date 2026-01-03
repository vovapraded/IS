package org.example.domain.route.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    @PersistenceContext(unitName = "RoutesPU")
    private EntityManager em;

    public RouteDto createRoute(RouteCreateDto dto) {
        log.info("Creating route {}", dto);
        
        // Создаем координаты и локации (owner будет установлен позже)
        CoordinatesDto coordsDto = coordinatesService.findOrCreate(dto.coordinates());
        LocationDto fromDto = locationService.findOrCreate(dto.from());
        LocationDto toDto = locationService.findOrCreate(dto.to());
        
        // Создаем маршрут с установленными связями используя единый EntityManager
        Route entity = new Route();
        entity.setName(dto.name());
        entity.setDistance(dto.distance());
        entity.setRating(dto.rating());
        
        // Используем единый EntityManager для загрузки всех связанных объектов
        entity.setCoordinates(em.find(org.example.domain.coordinates.entity.Coordinates.class, coordsDto.id()));
        entity.setFrom(em.find(org.example.domain.location.entity.Location.class, fromDto.id()));
        entity.setTo(em.find(org.example.domain.location.entity.Location.class, toDto.id()));
        
        // Сохраняем маршрут
        Route saved = routeRepository.save(entity);
        
        // Обновляем владельца для координат и локаций
        coordinatesService.updateOwner(saved.getCoordinates().getId(), saved);
        locationService.updateOwner(saved.getFrom().getId(), saved);
        locationService.updateOwner(saved.getTo().getId(), saved);
        
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

        // В новой модели владения проверяем, является ли удаляемый маршрут владельцем
        boolean isCoordinatesOwner = routeToDelete.getCoordinates().getOwnerRoute() != null &&
                                    routeToDelete.getCoordinates().getOwnerRoute().getId().equals(id);
        boolean isFromLocationOwner = routeToDelete.getFrom().getOwnerRoute() != null &&
                                     routeToDelete.getFrom().getOwnerRoute().getId().equals(id);
        boolean isToLocationOwner = routeToDelete.getTo().getOwnerRoute() != null &&
                                   routeToDelete.getTo().getOwnerRoute().getId().equals(id);
        
        // Подсчитываем использование только для объектов, которыми владеет удаляемый маршрут
        long coordinatesUsageCount = isCoordinatesOwner ?
            coordinatesService.getUsageCountExcluding(routeToDelete.getCoordinates().getId(), id) : 0;
        long fromLocationUsageCount = isFromLocationOwner ?
            locationService.getUsageCountExcluding(routeToDelete.getFrom().getId(), id) : 0;
        long toLocationUsageCount = isToLocationOwner ?
            locationService.getUsageCountExcluding(routeToDelete.getTo().getId(), id) : 0;

        // Нужна перепривязка только если маршрут владеет объектом И этот объект используется в других маршрутах
        boolean needsOwnershipTransfer = (isCoordinatesOwner && coordinatesUsageCount > 0) ||
                                        (isFromLocationOwner && fromLocationUsageCount > 0) ||
                                        (isToLocationOwner && toLocationUsageCount > 0);
        
        Map<String, Object> result = new HashMap<>();
        result.put("needsOwnershipTransfer", needsOwnershipTransfer);
        result.put("isCoordinatesOwner", isCoordinatesOwner);
        result.put("isFromLocationOwner", isFromLocationOwner);
        result.put("isToLocationOwner", isToLocationOwner);
        result.put("coordinatesUsageCount", coordinatesUsageCount);
        result.put("fromLocationUsageCount", fromLocationUsageCount);
        result.put("toLocationUsageCount", toLocationUsageCount);
        result.put("route", RouteMapper.toDto(routeToDelete));
        
        if (needsOwnershipTransfer) {
            // Получаем кандидатов для передачи владения
            List<RouteDto> coordinatesCandidates = java.util.Collections.emptyList();
            List<RouteDto> fromLocationCandidates = java.util.Collections.emptyList();
            List<RouteDto> toLocationCandidates = java.util.Collections.emptyList();
            
            if (coordinatesUsageCount > 0) {
                coordinatesCandidates = routeRepository.findAll().stream()
                    .filter(r -> !r.getId().equals(id))
                    .filter(r -> r.getCoordinates().getId().equals(routeToDelete.getCoordinates().getId()))
                    .map(RouteMapper::toDto)
                    .collect(Collectors.toList());
            }
            
            if (fromLocationUsageCount > 0) {
                fromLocationCandidates = routeRepository.findAll().stream()
                    .filter(r -> !r.getId().equals(id))
                    .filter(r -> r.getFrom().getId().equals(routeToDelete.getFrom().getId()) ||
                                 r.getTo().getId().equals(routeToDelete.getFrom().getId()))
                    .map(RouteMapper::toDto)
                    .collect(Collectors.toList());
            }
            
            if (toLocationUsageCount > 0) {
                toLocationCandidates = routeRepository.findAll().stream()
                    .filter(r -> !r.getId().equals(id))
                    .filter(r -> r.getFrom().getId().equals(routeToDelete.getTo().getId()) ||
                                 r.getTo().getId().equals(routeToDelete.getTo().getId()))
                    .map(RouteMapper::toDto)
                    .collect(Collectors.toList());
            }
            
            result.put("coordinatesCandidates", coordinatesCandidates);
            result.put("fromLocationCandidates", fromLocationCandidates);
            result.put("toLocationCandidates", toLocationCandidates);
        }

        log.info("Route {} ownership check - needs transfer: {}, owns: coords={}, from={}, to={}",
            id, needsOwnershipTransfer, isCoordinatesOwner, isFromLocationOwner, isToLocationOwner);

        return result;
    }

    @Transactional
    public void delete(Integer id) {
        log.info("Deleting route with id {}", id);
        
        // Проверяем зависимости для автоматического определения необходимости передачи владения
        Map<String, Object> dependencies = checkDependencies(id);
        boolean needsOwnershipTransfer = (Boolean) dependencies.get("needsOwnershipTransfer");
        
        if (needsOwnershipTransfer) {
            // Автоматически передаем владение первым доступным кандидатам
            Integer coordinatesTargetId = null;
            Integer fromLocationTargetId = null;
            Integer toLocationTargetId = null;
            
            @SuppressWarnings("unchecked")
            List<RouteDto> coordinatesCandidates = (List<RouteDto>) dependencies.get("coordinatesCandidates");
            if (coordinatesCandidates != null && !coordinatesCandidates.isEmpty()) {
                coordinatesTargetId = coordinatesCandidates.get(0).id();
            }
            
            @SuppressWarnings("unchecked")
            List<RouteDto> fromLocationCandidates = (List<RouteDto>) dependencies.get("fromLocationCandidates");
            if (fromLocationCandidates != null && !fromLocationCandidates.isEmpty()) {
                fromLocationTargetId = fromLocationCandidates.get(0).id();
            }
            
            @SuppressWarnings("unchecked")
            List<RouteDto> toLocationCandidates = (List<RouteDto>) dependencies.get("toLocationCandidates");
            if (toLocationCandidates != null && !toLocationCandidates.isEmpty()) {
                toLocationTargetId = toLocationCandidates.get(0).id();
            }
            
            deleteWithOwnershipTransfer(id, coordinatesTargetId, fromLocationTargetId, toLocationTargetId);
        } else {
            // Простое удаление без передачи владения
            deleteWithoutOwnershipTransfer(id);
        }
    }

    @Transactional
    public void deleteWithRebinding(Integer id, Integer coordinatesTargetRouteId,
                                   Integer fromLocationTargetRouteId, Integer toLocationTargetRouteId) {
        log.info("Deleting route with id {} and rebinding: coordinates -> {}, from -> {}, to -> {}",
            id, coordinatesTargetRouteId, fromLocationTargetRouteId, toLocationTargetRouteId);
        
        // ИСПОЛЬЗУЕМ ЕДИНЫЙ EntityManager для всех операций
        Route routeToDelete = em.find(Route.class, id);
        if (routeToDelete == null) {
            throw new IllegalArgumentException("Route not found with id: " + id);
        }

        // Получаем связанные объекты через единый EntityManager
        org.example.domain.coordinates.entity.Coordinates coordinates = routeToDelete.getCoordinates();
        org.example.domain.location.entity.Location fromLocation = routeToDelete.getFrom();
        org.example.domain.location.entity.Location toLocation = routeToDelete.getTo();

        // Передаем владение координатами указанному целевому маршруту
        if (coordinatesTargetRouteId != null) {
            Route coordinatesTargetRoute = em.find(Route.class, coordinatesTargetRouteId);
            if (coordinatesTargetRoute == null) {
                throw new IllegalArgumentException("Coordinates target route not found with id: " + coordinatesTargetRouteId);
            }
            
            // Передаем владение координатами через единый EntityManager
            coordinates.setOwnerRoute(coordinatesTargetRoute);
            em.merge(coordinates);
            
            log.info("Transferred coordinates ownership from route {} to route {}", id, coordinatesTargetRouteId);
        } else {
            // Если целевой маршрут не указан, очищаем владение
            coordinates.setOwnerRoute(null);
            em.merge(coordinates);
        }

        // Передаем владение локацией from указанному целевому маршруту
        if (fromLocationTargetRouteId != null) {
            Route fromTargetRoute = em.find(Route.class, fromLocationTargetRouteId);
            if (fromTargetRoute == null) {
                throw new IllegalArgumentException("From location target route not found with id: " + fromLocationTargetRouteId);
            }
            
            // Передаем владение локацией через единый EntityManager
            fromLocation.setOwnerRoute(fromTargetRoute);
            em.merge(fromLocation);
            
            log.info("Transferred from location ownership from route {} to route {}", id, fromLocationTargetRouteId);
        } else {
            // Если целевой маршрут не указан, очищаем владение
            fromLocation.setOwnerRoute(null);
            em.merge(fromLocation);
        }

        // Передаем владение локацией to указанному целевому маршруту
        if (toLocationTargetRouteId != null) {
            Route toTargetRoute = em.find(Route.class, toLocationTargetRouteId);
            if (toTargetRoute == null) {
                throw new IllegalArgumentException("To location target route not found with id: " + toLocationTargetRouteId);
            }
            
            // Передаем владение локацией через единый EntityManager
            toLocation.setOwnerRoute(toTargetRoute);
            em.merge(toLocation);
            
            log.info("Transferred to location ownership from route {} to route {}", id, toLocationTargetRouteId);
        } else {
            // Если целевой маршрут не указан, очищаем владение
            toLocation.setOwnerRoute(null);
            em.merge(toLocation);
        }

        // КРИТИЧЕСКИ ВАЖНО: принудительно сохраняем все изменения владения
        em.flush();

        // Теперь безопасно удаляем маршрут через единый EntityManager
        em.remove(routeToDelete);

        // Очищаем неиспользуемые ресурсы после успешного удаления
        if (coordinatesTargetRouteId == null) {
            cleanupUnusedCoordinates(coordinates.getId());
        }
        if (fromLocationTargetRouteId == null) {
            cleanupUnusedLocation(fromLocation.getId());
        }
        if (toLocationTargetRouteId == null) {
            cleanupUnusedLocation(toLocation.getId());
        }

        log.info("Route {} successfully deleted with separate rebinding", id);
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
        
        // Используем существующий метод createRoute с правильной структурой
        CoordinatesDto coordinatesDto = new CoordinatesDto(null, coordX, coordY, null, null);
        LocationDto fromDto = new LocationDto(null, fromX, fromY, fromName, null, null);
        LocationDto toDto = new LocationDto(null, toX, toY, toName, null, null);
        
        RouteCreateDto routeCreateDto = new RouteCreateDto(
            routeName, coordinatesDto, fromDto, toDto, distance, rating
        );
        
        return createRoute(routeCreateDto);
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
    
    // Новые методы для работы с владением
    
    public CoordinatesDto getOrCreateCoordinatesWithOwner(CoordinatesDto coordinatesDto, Route ownerRoute) {
        return coordinatesService.findOrCreateWithOwner(coordinatesDto, ownerRoute);
    }

    public LocationDto getOrCreateLocationWithOwner(LocationDto locationDto, Route ownerRoute) {
        return locationService.findOrCreateWithOwner(locationDto, ownerRoute);
    }
    
    @Transactional
    private void deleteWithOwnershipTransfer(Integer id, Integer coordinatesTargetId,
                                           Integer fromLocationTargetId, Integer toLocationTargetId) {
        log.info("Deleting route {} with ownership transfer: coords->{}, from->{}, to->{}",
            id, coordinatesTargetId, fromLocationTargetId, toLocationTargetId);
        
        // Загружаем все объекты через ЕДИНЫЙ EntityManager
        Route routeToDelete = em.find(Route.class, id);
        if (routeToDelete == null) {
            throw new IllegalArgumentException("Route not found with id: " + id);
        }

        // Получаем связанные объекты через единый EntityManager
        org.example.domain.coordinates.entity.Coordinates coordinates = routeToDelete.getCoordinates();
        org.example.domain.location.entity.Location fromLocation = routeToDelete.getFrom();
        org.example.domain.location.entity.Location toLocation = routeToDelete.getTo();
        
        // Передаем владение координатами
        if (coordinatesTargetId != null) {
            Route coordinatesTarget = em.find(Route.class, coordinatesTargetId);
            if (coordinatesTarget != null) {
                coordinates.setOwnerRoute(coordinatesTarget);
                em.merge(coordinates);
                log.info("Transferred coordinates ownership to route {}", coordinatesTargetId);
            }
        } else {
            // Очищаем владение
            coordinates.setOwnerRoute(null);
            em.merge(coordinates);
        }
        
        // Передаем владение from локацией
        if (fromLocationTargetId != null) {
            Route fromTarget = em.find(Route.class, fromLocationTargetId);
            if (fromTarget != null) {
                fromLocation.setOwnerRoute(fromTarget);
                em.merge(fromLocation);
                log.info("Transferred from location ownership to route {}", fromLocationTargetId);
            }
        } else {
            fromLocation.setOwnerRoute(null);
            em.merge(fromLocation);
        }
        
        // Передаем владение to локацией
        if (toLocationTargetId != null) {
            Route toTarget = em.find(Route.class, toLocationTargetId);
            if (toTarget != null) {
                toLocation.setOwnerRoute(toTarget);
                em.merge(toLocation);
                log.info("Transferred to location ownership to route {}", toLocationTargetId);
            }
        } else {
            toLocation.setOwnerRoute(null);
            em.merge(toLocation);
        }
        
        // Принудительно сохраняем все изменения владения
        em.flush();
        
        // Теперь безопасно удаляем маршрут через EntityManager
        em.remove(routeToDelete);
        
        log.info("Route {} deleted with ownership transfer completed", id);
    }
    
    @Transactional
    private void deleteWithoutOwnershipTransfer(Integer id) {
        log.info("Deleting route {} without ownership transfer", id);
        
        // Загружаем объект через единый EntityManager
        Route routeToDelete = em.find(Route.class, id);
        if (routeToDelete == null) {
            throw new IllegalArgumentException("Route not found with id: " + id);
        }

        // Получаем связанные объекты через единый EntityManager
        org.example.domain.coordinates.entity.Coordinates coordinates = routeToDelete.getCoordinates();
        org.example.domain.location.entity.Location fromLocation = routeToDelete.getFrom();
        org.example.domain.location.entity.Location toLocation = routeToDelete.getTo();
        
        // Запоминаем ID для возможной очистки
        Integer coordinatesId = coordinates.getId();
        Integer fromLocationId = fromLocation.getId();
        Integer toLocationId = toLocation.getId();
        
        boolean isCoordinatesOwner = coordinates.getOwnerRoute() != null &&
                                    coordinates.getOwnerRoute().getId().equals(id);
        boolean isFromLocationOwner = fromLocation.getOwnerRoute() != null &&
                                     fromLocation.getOwnerRoute().getId().equals(id);
        boolean isToLocationOwner = toLocation.getOwnerRoute() != null &&
                                   toLocation.getOwnerRoute().getId().equals(id);

        // Очищаем владение перед удалением маршрута через единый EntityManager
        if (isCoordinatesOwner) {
            coordinates.setOwnerRoute(null);
            em.merge(coordinates);
        }
        if (isFromLocationOwner) {
            fromLocation.setOwnerRoute(null);
            em.merge(fromLocation);
        }
        if (isToLocationOwner) {
            toLocation.setOwnerRoute(null);
            em.merge(toLocation);
        }

        // Принудительно сохраняем изменения
        em.flush();
        
        // Удаляем маршрут через EntityManager
        em.remove(routeToDelete);

        // Удаляем объекты, которыми владел этот маршрут, если они больше не используются
        if (isCoordinatesOwner) {
            cleanupUnusedCoordinates(coordinatesId);
        }
        if (isFromLocationOwner) {
            cleanupUnusedLocation(fromLocationId);
        }
        if (isToLocationOwner) {
            cleanupUnusedLocation(toLocationId);
        }

        log.info("Route {} deleted without ownership transfer", id);
    }
}