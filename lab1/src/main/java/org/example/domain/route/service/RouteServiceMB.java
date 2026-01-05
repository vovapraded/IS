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
import org.example.domain.route.dto.RouteCursorPageDto;
import org.example.exception.RouteNameAlreadyExistsException;
import org.example.exception.RouteCoordinatesAlreadyExistException;

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

    // Методы валидации уникальности
    
    /**
     * Проверяет уникальность имени маршрута при создании (в рамках транзакции)
     */
    private void validateRouteNameUniquenessInTransaction(String name) {
        log.info("VALIDATION: Checking route name uniqueness in transaction for: '{}'", name);
        if (name == null || name.trim().isEmpty()) {
            log.info("VALIDATION: Name is empty, skipping uniqueness check");
            return; // пустые имена не проверяем
        }
        
        // Проверяем прямо в EntityManager в текущей транзакции
        log.info("VALIDATION: Searching for existing route with name in transaction: '{}'", name.trim());
        List<Route> existingRoutes = em.createQuery(
            "SELECT r FROM Route r WHERE r.name = :name", Route.class)
            .setParameter("name", name.trim())
            .setMaxResults(1)
            .getResultList();
            
        if (!existingRoutes.isEmpty()) {
            Route existingRoute = existingRoutes.get(0);
            log.error("VALIDATION: Route with name '{}' already exists with ID: {}", name.trim(), existingRoute.getId());
            throw new RouteNameAlreadyExistsException(name.trim(), existingRoute.getId());
        }
        log.info("VALIDATION: Route name '{}' is unique in transaction", name.trim());
    }
    
    /**
     * Проверяет уникальность имени маршрута при обновлении
     */
    private void validateRouteNameUniquenessForUpdate(String name, Integer excludeRouteId) {
        if (name == null || name.trim().isEmpty()) {
            return; // пустые имена не проверяем
        }
        
        Route existingRoute = routeRepository.findByNameExcluding(name.trim(), excludeRouteId);
        if (existingRoute != null) {
            throw new RouteNameAlreadyExistsException(name.trim(), existingRoute.getId());
        }
    }
    
    /**
     * Проверяет уникальность координат маршрута при создании (в рамках транзакции)
     */
    private void validateRouteCoordinatesUniquenessInTransaction(Double x, Double y) {
        log.info("COORDINATES VALIDATION: Checking coordinates uniqueness in transaction for: ({}, {})", x, y);
        if (x == null || y == null) {
            log.info("COORDINATES VALIDATION: X or Y coordinate is null, skipping uniqueness check");
            return; // некорректные координаты не проверяем
        }
        
        try {
            // Проверяем прямо в EntityManager в текущей транзакции
            log.info("COORDINATES VALIDATION: Searching for existing route with coordinates in transaction: ({}, {})", x, y);
            List<Route> existingRoutes = em.createQuery(
                "SELECT r FROM Route r WHERE r.coordinates.x = :x AND r.coordinates.y = :y", Route.class)
                .setParameter("x", x.floatValue())
                .setParameter("y", y)
                .setMaxResults(1)
                .getResultList();
            
            if (!existingRoutes.isEmpty()) {
                Route existingRoute = existingRoutes.get(0);
                log.error("COORDINATES VALIDATION: Route with coordinates ({}, {}) already exists with ID: {}",
                        x, y, existingRoute.getId());
                RouteCoordinatesAlreadyExistException exception = new RouteCoordinatesAlreadyExistException(x.floatValue(), y, existingRoute.getId());
                log.error("COORDINATES VALIDATION: Throwing exception: {}", exception.getMessage());
                throw exception;
            }
            log.info("COORDINATES VALIDATION: Coordinates ({}, {}) are unique in transaction", x, y);
        } catch (RouteCoordinatesAlreadyExistException e) {
            log.error("COORDINATES VALIDATION: Re-throwing coordinates exception: {}", e.getMessage());
            throw e; // Перебрасываем наше исключение
        } catch (Exception e) {
            log.error("COORDINATES VALIDATION: Unexpected error during coordinates validation: {}", e.getMessage(), e);
            throw new RuntimeException("Error during coordinates validation: " + e.getMessage(), e);
        }
    }
    
    /**
     * Проверяет уникальность координат маршрута при обновлении
     */
    private void validateRouteCoordinatesUniquenessForUpdate(Double x, Double y, Integer excludeRouteId) {
        log.info("UPDATE COORDINATES VALIDATION: Checking coordinates uniqueness for update: ({}, {}), excluding route ID: {}",
                x, y, excludeRouteId);
        if (x == null || y == null) {
            log.info("UPDATE COORDINATES VALIDATION: X or Y coordinate is null, skipping uniqueness check");
            return; // некорректные координаты не проверяем
        }
        
        try {
            log.info("UPDATE COORDINATES VALIDATION: Searching for existing route with coordinates: ({}, {}) excluding route: {}",
                    x, y, excludeRouteId);
            Route existingRoute = routeRepository.findByCoordinatesExcluding(x, y, excludeRouteId);
            
            if (existingRoute != null) {
                log.error("UPDATE COORDINATES VALIDATION: Route with coordinates ({}, {}) already exists with ID: {} (excluding: {})",
                        x, y, existingRoute.getId(), excludeRouteId);
                RouteCoordinatesAlreadyExistException exception = new RouteCoordinatesAlreadyExistException(x.floatValue(), y, existingRoute.getId());
                log.error("UPDATE COORDINATES VALIDATION: Throwing exception: {}", exception.getMessage());
                throw exception;
            }
            log.info("UPDATE COORDINATES VALIDATION: Coordinates ({}, {}) are unique for update", x, y);
        } catch (RouteCoordinatesAlreadyExistException e) {
            log.error("UPDATE COORDINATES VALIDATION: Re-throwing coordinates exception: {}", e.getMessage());
            throw e; // Перебрасываем наше исключение
        } catch (Exception e) {
            log.error("UPDATE COORDINATES VALIDATION: Unexpected error during coordinates validation: {}", e.getMessage(), e);
            throw new RuntimeException("Error during coordinates validation for update: " + e.getMessage(), e);
        }
    }

    @Transactional
    public RouteDto createRoute(RouteCreateDto dto) {
        log.info("SERVICE: Starting route creation: {}", dto);
        
        try {
            // Проверяем уникальность имени маршрута в транзакции
            log.info("SERVICE: Validating route name uniqueness in transaction: {}", dto.name());
            validateRouteNameUniquenessInTransaction(dto.name());
            log.info("SERVICE: Name validation passed");
            
            // Проверяем уникальность координат маршрута в транзакции
            if (dto.coordinates() != null) {
                log.info("SERVICE: Validating route coordinates uniqueness in transaction: ({}, {})", dto.coordinates().x(), dto.coordinates().y());
                validateRouteCoordinatesUniquenessInTransaction((double)dto.coordinates().x(), dto.coordinates().y());
                log.info("SERVICE: Coordinates validation passed");
            }
            
            log.info("SERVICE: All validation passed, proceeding with route creation");
            
            // Создаем координаты и локации (owner будет установлен позже)
            log.info("SERVICE: Creating coordinates...");
            CoordinatesDto coordsDto = coordinatesService.findOrCreate(dto.coordinates());
            log.info("SERVICE: Coordinates created with ID: {}", coordsDto.id());
            
            log.info("SERVICE: Creating from location...");
            LocationDto fromDto = locationService.findOrCreate(dto.from());
            log.info("SERVICE: From location created with ID: {}", fromDto.id());
            
            log.info("SERVICE: Creating to location...");
            LocationDto toDto = locationService.findOrCreate(dto.to());
            log.info("SERVICE: To location created with ID: {}", toDto.id());
            
            // Создаем маршрут с установленными связями используя единый EntityManager
            log.info("SERVICE: Creating route entity...");
            Route entity = new Route();
            entity.setName(dto.name());
            entity.setDistance(dto.distance());
            entity.setRating(dto.rating());
            
            // Используем единый EntityManager для загрузки всех связанных объектов
            log.info("SERVICE: Loading coordinates entity with ID: {}", coordsDto.id());
            org.example.domain.coordinates.entity.Coordinates coordsEntity = em.find(org.example.domain.coordinates.entity.Coordinates.class, coordsDto.id());
            if (coordsEntity == null) {
                throw new IllegalStateException("Failed to find coordinates entity with ID: " + coordsDto.id());
            }
            entity.setCoordinates(coordsEntity);
            
            log.info("SERVICE: Loading from location entity with ID: {}", fromDto.id());
            org.example.domain.location.entity.Location fromEntity = em.find(org.example.domain.location.entity.Location.class, fromDto.id());
            if (fromEntity == null) {
                throw new IllegalStateException("Failed to find from location entity with ID: " + fromDto.id());
            }
            entity.setFrom(fromEntity);
            
            log.info("SERVICE: Loading to location entity with ID: {}", toDto.id());
            org.example.domain.location.entity.Location toEntity = em.find(org.example.domain.location.entity.Location.class, toDto.id());
            if (toEntity == null) {
                throw new IllegalStateException("Failed to find to location entity with ID: " + toDto.id());
            }
            entity.setTo(toEntity);
            
            // Сохраняем маршрут
            log.info("SERVICE: Saving route entity...");
            Route saved = routeRepository.save(entity);
            log.info("SERVICE: Route saved with ID: {}", saved.getId());
            
            // Обновляем владельца для координат и локаций
            try {
                log.info("SERVICE: Updating coordinates owner...");
                coordinatesService.updateOwner(saved.getCoordinates().getId(), saved);
                
                log.info("SERVICE: Updating from location owner...");
                locationService.updateOwner(saved.getFrom().getId(), saved);
                
                log.info("SERVICE: Updating to location owner...");
                locationService.updateOwner(saved.getTo().getId(), saved);
            } catch (Exception ownerUpdateException) {
                log.error("SERVICE: Failed to update owners, but route was created. Route ID: {}, Error: {}",
                         saved.getId(), ownerUpdateException.getMessage(), ownerUpdateException);
                // Продолжаем выполнение - маршрут создан, проблемы с владельцем не критичны
            }
            
            RouteDto result = RouteMapper.toDto(saved);
            log.info("SERVICE: Route successfully created with id: {}", result.id());
            return result;
            
        } catch (RouteNameAlreadyExistsException | RouteCoordinatesAlreadyExistException e) {
            log.error("SERVICE: Validation error during route creation: {}", e.getMessage());
            throw e; // Перебрасываем валидационные исключения как есть
        } catch (Exception e) {
            log.error("SERVICE: Unexpected error during route creation - Type: {}, Message: {}",
                     e.getClass().getName(), e.getMessage(), e);
            // Оборачиваем в RuntimeException с подробной информацией
            throw new RuntimeException("Failed to create route: " + e.getMessage(), e);
        }
    }

    public RouteDto findById(Integer id) {
        Route route = routeRepository.findById(id);
        if (route == null) {
            throw new IllegalArgumentException("Route not found with id: " + id);
        }
        return RouteMapper.toDto(route);
    }

    /**
     * Простой метод findAll без пагинации
     */
    public List<RouteDto> findAll() {
        log.info("Finding all routes");
        List<Route> routes = routeRepository.findAll();
        return routes.stream()
                .map(RouteMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Простая offset/limit пагинация (заменяет cursor пагинацию)
     */
    public List<RouteDto> findPaginated(int page, int size, String nameFilter, String sortBy, String sortDirection) {
        log.info("Finding paginated routes: page={}, size={}, filter='{}', sortBy={}, direction={}",
                page, size, nameFilter, sortBy, sortDirection);
        
        List<Route> routes = routeRepository.findPaginated(page, size, nameFilter, sortBy, sortDirection);
        return routes.stream()
                .map(RouteMapper::toDto)
                .collect(Collectors.toList());
    }

    public long countAll() {
        return routeRepository.countAll();
    }

    public long countWithFilter(String nameFilter) {
        return routeRepository.countWithFilter(nameFilter);
    }

    // Основная пагинация
    
    /**
     * Получить первую страницу маршрутов
     */
    public RouteCursorPageDto findFirstPage(int size, String nameFilter) {
        return findFirstPage(size, nameFilter, "id", "asc");
    }
    
    public RouteCursorPageDto findFirstPage(int size, String nameFilter, String sortBy, String sortDirection) {
        log.info("Finding first page: size={}, filter='{}', sortBy={}, direction={}",
                size, nameFilter, sortBy, sortDirection);
        
        List<Route> routes = routeRepository.findFirstPage(size, nameFilter, sortBy, sortDirection);
        List<RouteDto> routeDtos = routes.stream()
                .map(RouteMapper::toDto)
                .collect(Collectors.toList());
        
        // Для первой страницы вычисляем общий count
        long totalCount = countWithFilter(nameFilter);
        
        return RouteCursorPageDto.first(routeDtos, size, totalCount, sortBy, sortDirection);
    }
    
    /**
     * Получить следующую страницу после указанного cursor'а (с композитными курсорами)
     */
    public RouteCursorPageDto findNextPage(String encodedCursor, int size, String nameFilter) {
        log.info("Finding next page: cursor={}, size={}, filter='{}'", encodedCursor, size, nameFilter);
        
        if (encodedCursor == null || !encodedCursor.startsWith("id:")) {
            return findFirstPage(size, nameFilter, "id", "asc");
        }
        
        try {
            // Парсим простой cursor: "id:123"
            Integer cursorId = Integer.parseInt(encodedCursor.substring(3));
            
            List<Route> routes = routeRepository.findFirstPage(size + 1, nameFilter, "id", "asc");
            // Фильтруем routes после cursor
            List<Route> filteredRoutes = routes.stream()
                    .filter(route -> route.getId() > cursorId)
                    .limit(size)
                    .collect(Collectors.toList());
            
            List<RouteDto> routeDtos = filteredRoutes.stream()
                    .map(RouteMapper::toDto)
                    .collect(Collectors.toList());
            
            // Вычисляем общий count для корректной работы UI
            long totalCount = countWithFilter(nameFilter);
            return RouteCursorPageDto.next(routeDtos, size, totalCount, "id", "asc");
            
        } catch (Exception e) {
            log.warn("Invalid cursor format, returning first page: {}", e.getMessage());
            return findFirstPage(size, nameFilter, "id", "asc");
        }
    }
    
    public RouteCursorPageDto findNextPage(CompositeCursor cursor, int size, String nameFilter) {
        // Для совместимости - используем простой cursor
        return findNextPage("id:" + cursor.id(), size, nameFilter);
    }
    
    /**
     * Получить предыдущую страницу до указанного cursor'а (с композитными курсорами)
     */
    public RouteCursorPageDto findPrevPage(String encodedCursor, int size, String nameFilter) {
        log.info("Finding prev page: cursor={}, size={}, filter='{}'", encodedCursor, size, nameFilter);
        
        if (encodedCursor == null || !encodedCursor.startsWith("id:")) {
            return findFirstPage(size, nameFilter, "id", "asc");
        }
        
        try {
            // Парсим простой cursor: "id:123"
            Integer cursorId = Integer.parseInt(encodedCursor.substring(3));
            
            List<Route> routes = routeRepository.findFirstPage(size + 1, nameFilter, "id", "desc");
            // Фильтруем routes до cursor и разворачиваем обратно
            List<Route> filteredRoutes = routes.stream()
                    .filter(route -> route.getId() < cursorId)
                    .limit(size)
                    .sorted((r1, r2) -> Integer.compare(r1.getId(), r2.getId())) // Сортируем обратно по возрастанию
                    .collect(Collectors.toList());
            
            List<RouteDto> routeDtos = filteredRoutes.stream()
                    .map(RouteMapper::toDto)
                    .collect(Collectors.toList());
            
            // Вычисляем общий count для корректной работы UI
            long totalCount = countWithFilter(nameFilter);
            return RouteCursorPageDto.prev(routeDtos, size, totalCount, "id", "asc");
            
        } catch (Exception e) {
            log.warn("Invalid cursor format, returning first page: {}", e.getMessage());
            return findFirstPage(size, nameFilter, "id", "asc");
        }
    }
    
    public RouteCursorPageDto findPrevPage(CompositeCursor cursor, int size, String nameFilter) {
        // Для совместимости - используем простой cursor
        return findPrevPage("id:" + cursor.id(), size, nameFilter);
    }
    
    /**
     * Универсальный метод пагинации с композитными курсорами
     */
    public RouteCursorPageDto findPage(String encodedCursor, int size, String nameFilter,
                                      String sortBy, String sortDirection) {
        if (encodedCursor == null || encodedCursor.trim().isEmpty()) {
            // Первая страница
            return findFirstPage(size, nameFilter, sortBy, sortDirection);
        } else {
            // Последующие страницы - пока только для ID сортировки
            return findNextPage(encodedCursor, size, nameFilter);
        }
    }
    

    public RouteDto updateRoute(RouteUpdateDto dto) {
        log.info("Updating route {}", dto);
        try {
            // Проверяем уникальность имени маршрута при обновлении
            if (dto.name() != null) {
                validateRouteNameUniquenessForUpdate(dto.name(), dto.id());
            }
            
            // Проверяем уникальность координат маршрута при обновлении
            // Для этого нужно получить текущий маршрут и проверить изменения координат
            Route currentRoute = routeRepository.findById(dto.id());
            if (currentRoute != null && dto.coordinates() != null) {
                validateRouteCoordinatesUniquenessForUpdate(
                    (double)dto.coordinates().x(),
                    dto.coordinates().y(),
                    dto.id()
                );
            }
            
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
            
            // ИСПРАВЛЕНО: используем специальные методы вместо findAll().filter()
            if (coordinatesUsageCount > 0) {
                coordinatesCandidates = routeRepository.findByCoordinatesIdExcluding(
                    routeToDelete.getCoordinates().getId(), id).stream()
                    .map(RouteMapper::toDto)
                    .collect(Collectors.toList());
            }
            
            if (fromLocationUsageCount > 0) {
                fromLocationCandidates = routeRepository.findByLocationIdExcluding(
                    routeToDelete.getFrom().getId(), id).stream()
                    .map(RouteMapper::toDto)
                    .collect(Collectors.toList());
            }
            
            if (toLocationUsageCount > 0) {
                toLocationCandidates = routeRepository.findByLocationIdExcluding(
                    routeToDelete.getTo().getId(), id).stream()
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

    public RouteDto addRouteBetweenLocations(String routeName, Double coordX, Double coordY,
                                           Double fromX, double fromY, String fromName,
                                           Double toX, double toY, String toName,
                                           Long distance, Long rating) {
        log.info("Adding route between locations {} and {}", fromName, toName);
        
        // Проверяем уникальность имени маршрута в транзакции
        validateRouteNameUniquenessInTransaction(routeName);
        
        // Проверяем уникальность координат маршрута в транзакции
        validateRouteCoordinatesUniquenessInTransaction(coordX, coordY);
        
        // Используем существующий метод createRoute с правильной структурой
        CoordinatesDto coordinatesDto = new CoordinatesDto(null, coordX.floatValue(), coordY, null, null);
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