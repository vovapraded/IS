package org.example.domain.route.service;

import jakarta.ejb.AccessTimeout;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.concurrent.TimeUnit;
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
import org.example.exception.RouteZeroDistanceException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Stateless
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@AccessTimeout(value = 30, unit = TimeUnit.SECONDS)
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
    
    
    /**
     * Проверяет, что маршрут не является "нулевым" (начальная и конечная точки не совпадают)
     */
    private void validateZeroDistanceRoute(Double fromX, Double fromY, Double toX, Double toY) {
        log.info("ZERO_DISTANCE VALIDATION: Checking that route is not zero distance: from=({}, {}) to=({}, {})", fromX, fromY, toX, toY);
        if (fromX == null || fromY == null || toX == null || toY == null) {
            log.info("ZERO_DISTANCE VALIDATION: Some coordinates are null, skipping zero distance check");
            return; // некорректные координаты не проверяем
        }
        
        // Проверяем, что начальная и конечная точки не совпадают с учетом погрешности для чисел с плавающей точкой
        // ИСПРАВЛЕНО: Увеличил epsilon до более разумного значения для координат
        double epsilon = 1e-6; // Более разумная величина для сравнения координат
        
        // ДЕТАЛЬНОЕ ЛОГИРОВАНИЕ ДЛЯ ОТЛАДКИ
        double diffX = Math.abs(fromX - toX);
        double diffY = Math.abs(fromY - toY);
        boolean sameX = diffX < epsilon;
        boolean sameY = diffY < epsilon;
        
        log.info("ZERO_DISTANCE VALIDATION DETAILS: diffX={}, diffY={}, epsilon={}, sameX={}, sameY={}",
                diffX, diffY, epsilon, sameX, sameY);
        
        // ИСПРАВЛЕНО: Добавлена дополнительная проверка - точки считаются одинаковыми только если расстояние между ними меньше epsilon
        double distance = Math.sqrt(Math.pow(diffX, 2) + Math.pow(diffY, 2));
        boolean isZeroDistance = distance < epsilon;
        
        log.info("ZERO_DISTANCE VALIDATION: Distance between points = {}, isZeroDistance = {}", distance, isZeroDistance);
        
        if (isZeroDistance) {
            log.error("ZERO_DISTANCE VALIDATION: Route has identical start and end points: from=({}, {}) to=({}, {})", fromX, fromY, toX, toY);
            log.error("ZERO_DISTANCE VALIDATION ERROR DETAILS: distance={} < epsilon={}", distance, epsilon);
            throw new RouteZeroDistanceException(fromX, fromY, toX, toY);
        }
        log.info("ZERO_DISTANCE VALIDATION: Route has different start and end points - validation passed");
    }
    
    /**
     * Проверяет уникальность имени маршрута при создании
     * Использует оптимистические блокировки вместо singleton EJB
     */
    private void validateRouteNameUniquenessForCreate(String name) {
        log.info("SERVICE: Checking route name uniqueness for create: '{}'", name);
        if (name == null || name.trim().isEmpty()) {
            log.info("SERVICE: Name is empty, skipping uniqueness check");
            return; // пустые имена не проверяем
        }
        
        String trimmedName = name.trim();
        log.info("SERVICE: Checking exact name match for create: '{}'", trimmedName);
        
        // Проверяем существование в БД с точным совпадением
        Route existing = routeRepository.findByName(trimmedName);
        if (existing != null) {
            log.error("SERVICE: Route with exact name '{}' already exists with ID: {}",
                     trimmedName, existing.getId());
            RouteDto conflictingRouteDto = RouteMapper.toDto(existing);
            log.info("SERVICE: Found conflicting RouteDto: {}", conflictingRouteDto);
            throw new RouteNameAlreadyExistsException(trimmedName, conflictingRouteDto);
        }
        
        log.info("SERVICE: Route name '{}' is unique for create", trimmedName);
    }

    /**
     * Проверяет уникальность имени маршрута при обновлении
     * Использует оптимистические блокировки вместо singleton EJB
     */
    private void validateRouteNameUniquenessForUpdate(String name, Integer excludeRouteId) {
        log.info("SERVICE: Checking route name uniqueness for update: '{}', excluding route ID: {}", name, excludeRouteId);
        if (name == null || name.trim().isEmpty()) {
            log.info("SERVICE: Name is empty, skipping uniqueness check");
            return; // пустые имена не проверяем
        }
        
        String trimmedName = name.trim();
        log.info("SERVICE: Checking exact name match for update: '{}'", trimmedName);
        
        // Проверяем существование в БД исключая обновляемый маршрут
        Route existing = routeRepository.findByNameExcluding(trimmedName, excludeRouteId);
        if (existing != null) {
            log.error("SERVICE: Route with exact name '{}' already exists with ID: {}, excluding: {}",
                     trimmedName, existing.getId(), excludeRouteId);
            RouteDto conflictingRouteDto = RouteMapper.toDto(existing);
            log.info("SERVICE: Found conflicting RouteDto: {}", conflictingRouteDto);
            throw new RouteNameAlreadyExistsException(trimmedName, conflictingRouteDto);
        }
        
        log.info("SERVICE: Route name '{}' is unique for update", trimmedName);
    }

    @Lock(LockType.WRITE)
    @AccessTimeout(value = 60, unit = TimeUnit.SECONDS)
    public RouteDto createRoute(RouteCreateDto dto) {
        log.info("SERVICE: Starting route creation: {}", dto);
        
        try {
            // Проверяем уникальность имени на уровне бизнес-логики
            log.info("SERVICE: Validating route name uniqueness: {}", dto.name());
            validateRouteNameUniquenessForCreate(dto.name());
            log.info("SERVICE: Name validation passed");
            
            // Проверяем, что маршрут не является "нулевым"
            if (dto.from() != null && dto.to() != null) {
                log.info("SERVICE: Validating that route is not zero distance: from=({}, {}) to=({}, {})",
                        dto.from().x(), dto.from().y(), dto.to().x(), dto.to().y());
                validateZeroDistanceRoute(dto.from().x(), dto.from().y(), dto.to().x(), dto.to().y());
                log.info("SERVICE: Zero distance validation passed");
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
            
        } catch (RouteNameAlreadyExistsException e) {
            log.error("SERVICE: Name validation error during route creation: {}", e.getMessage());
            throw e; // Перебрасываем валидационные исключения как есть
        } catch (RouteZeroDistanceException e) {
            log.error("SERVICE: Zero distance validation error during route creation: {}", e.getMessage());
            throw e; // Перебрасываем валидационные исключения как есть
        } catch (Exception e) {
            log.error("SERVICE: Unexpected error during route creation - Type: {}, Message: {}",
                     e.getClass().getName(), e.getMessage(), e);
            
            // Перехватываем constraint violations и конвертируем в 409 Conflict
            if (isConstraintViolation(e)) {
                log.error("SERVICE: Constraint violation detected, converting to 409 Conflict");
                throw convertConstraintViolationToConflict(e, dto);
            }
            
            // Оборачиваем в RuntimeException с подробной информацией
            throw new RuntimeException("Failed to create route: " + e.getMessage(), e);
        }
    }

    @Lock(LockType.READ)
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
    @Lock(LockType.READ)
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
    @Lock(LockType.READ)
    public List<RouteDto> findPaginated(int page, int size, String nameFilter, String sortBy, String sortDirection) {
        log.info("Finding paginated routes: page={}, size={}, filter='{}', sortBy={}, direction={}",
                page, size, nameFilter, sortBy, sortDirection);
        
        List<Route> routes = routeRepository.findPaginated(page, size, nameFilter, sortBy, sortDirection);
        return routes.stream()
                .map(RouteMapper::toDto)
                .collect(Collectors.toList());
    }

    @Lock(LockType.READ)
    public long countAll() {
        return routeRepository.countAll();
    }

    @Lock(LockType.READ)
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
    

    @Lock(LockType.WRITE)
    @AccessTimeout(value = 60, unit = TimeUnit.SECONDS)
    public RouteDto updateRoute(RouteUpdateDto dto) {
        log.info("UPDATE SERVICE: Starting route update for ID: {}", dto.id());
        
        try {
            log.info("UPDATE SERVICE: Validating route name uniqueness");
            // Проверяем уникальность имени на уровне бизнес-логики при обновлении
            if (dto.name() != null) {
                validateRouteNameUniquenessForUpdate(dto.name(), dto.id());
            }
            log.info("UPDATE SERVICE: Name validation passed");
            
            log.info("UPDATE SERVICE: Validating zero distance");
            // Проверяем, что обновляемый маршрут не станет "нулевым"
            if (dto.from() != null && dto.to() != null) {
                log.info("UPDATE SERVICE: Validating that updated route is not zero distance: from=({}, {}) to=({}, {})",
                        dto.from().x(), dto.from().y(), dto.to().x(), dto.to().y());
                validateZeroDistanceRoute(dto.from().x(), dto.from().y(), dto.to().x(), dto.to().y());
                log.info("UPDATE SERVICE: Zero distance validation passed");
            }
            
            log.info("UPDATE SERVICE: Calling repository updateFromDto");
            Route updated = routeRepository.updateFromDto(dto);
            log.info("UPDATE SERVICE: Repository update completed successfully");
            
            log.info("UPDATE SERVICE: Converting to DTO");
            RouteDto result = RouteMapper.toDto(updated);
            log.info("UPDATE SERVICE: Route update completed successfully for ID: {}", dto.id());
            return result;
            
        } catch (RouteNameAlreadyExistsException e) {
            log.error("UPDATE SERVICE: Name conflict during route update: {}", e.getMessage());
            throw e;
        } catch (RouteZeroDistanceException e) {
            log.error("UPDATE SERVICE: Zero distance validation failed during route update: {}", e.getMessage());
            throw e;
        } catch (jakarta.persistence.OptimisticLockException e) {
            log.error("UPDATE SERVICE: Optimistic lock exception during route update for ID: {} - concurrent modification detected", dto.id());
            throw new RuntimeException("Маршрут был изменен другим пользователем. Пожалуйста, обновите страницу и повторите операцию.", e);
        } catch (IllegalArgumentException e) {
            log.error("UPDATE SERVICE: Invalid argument during route update: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("UPDATE SERVICE: Unexpected error during route update - Type: {}, Message: {}, Cause: {}",
                     e.getClass().getSimpleName(), e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "null", e);
            
            // Проверяем, является ли это constraint violation
            if (isConstraintViolation(e)) {
                log.error("UPDATE SERVICE: Constraint violation detected during update, converting to appropriate exception");
                throw convertUpdateConstraintViolationToConflict(e, dto);
            }
            
            throw new RuntimeException("Ошибка при обновлении маршрута: " + e.getMessage(), e);
        }
    }

    @Lock(LockType.READ)
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

    @Lock(LockType.WRITE)
    @AccessTimeout(value = 60, unit = TimeUnit.SECONDS)
    public void delete(Integer id) {
        log.info("DELETE SERVICE: Starting deletion for route with id {}", id);
        
        // Проверка существования маршрута в самом начале
        Route routeExists = routeRepository.findById(id);
        if (routeExists == null) {
            log.warn("DELETE SERVICE: Route with id {} not found", id);
            throw new IllegalArgumentException("Route not found with id: " + id);
        }
        log.info("DELETE SERVICE: Route with id {} exists, proceeding with deletion", id);
        
        try {
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
        } catch (jakarta.persistence.OptimisticLockException e) {
            // ИСПРАВЛЕНО: Обработка OptimisticLockException при concurrent deletion
            log.info("OptimisticLockException during deletion of route {} - concurrent modification detected, checking if route still exists", id);
            
            // Проверяем, существует ли еще маршрут
            Route stillExists = routeRepository.findById(id);
            if (stillExists == null) {
                log.info("Route {} no longer exists after OptimisticLockException - successfully deleted by concurrent transaction", id);
                return; // Маршрут уже удален другой транзакцией
            } else {
                log.warn("Route {} still exists after OptimisticLockException - rethrowing", id);
                throw e; // Маршрут еще существует, это реальная проблема
            }
        }
    }

    @Lock(LockType.WRITE)
    @AccessTimeout(value = 60, unit = TimeUnit.SECONDS)
    public void deleteWithRebinding(Integer id, Integer coordinatesTargetRouteId,
                                   Integer fromLocationTargetRouteId, Integer toLocationTargetRouteId) {
        log.info("DELETE_REBIND SERVICE: Starting deletion with rebinding for route id {}, coordinates -> {}, from -> {}, to -> {}",
            id, coordinatesTargetRouteId, fromLocationTargetRouteId, toLocationTargetRouteId);
        
        // Проверка существования маршрута в самом начале
        Route routeExists = routeRepository.findById(id);
        if (routeExists == null) {
            log.warn("DELETE_REBIND SERVICE: Route with id {} not found", id);
            throw new IllegalArgumentException("Route not found with id: " + id);
        }
        log.info("DELETE_REBIND SERVICE: Route with id {} exists, proceeding with deletion", id);
        
        try {
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
        } catch (jakarta.persistence.OptimisticLockException e) {
            // ИСПРАВЛЕНО: Обработка OptimisticLockException при concurrent deletion с rebinding
            log.info("OptimisticLockException during deletion with rebinding for route {} - concurrent modification detected, checking if route still exists", id);
            
            // Проверяем, существует ли еще маршрут
            Route stillExists = routeRepository.findById(id);
            if (stillExists == null) {
                log.info("Route {} no longer exists after OptimisticLockException during rebinding - successfully deleted by concurrent transaction", id);
                return; // Маршрут уже удален другой транзакцией
            } else {
                log.warn("Route {} still exists after OptimisticLockException during rebinding - rethrowing", id);
                throw e; // Маршрут еще существует, это реальная проблема
            }
        }
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
    
    // Методы для обработки constraint violations
    
    /**
     * Проверяет, является ли исключение constraint violation
     */
    private boolean isConstraintViolation(Exception e) {
        if (e == null) return false;
        
        log.info("CONSTRAINT CHECK: Examining exception - Type: {}, Message: {}",
                e.getClass().getSimpleName(), e.getMessage());
        
        String message = e.getMessage();
        Throwable rootCause = e;
        
        // Проверяем всю цепочку причин
        while (rootCause != null) {
            String causeMessage = rootCause.getMessage();
            log.info("CONSTRAINT CHECK: Root cause - Type: {}, Message: {}",
                    rootCause.getClass().getSimpleName(), causeMessage);
            
            if (causeMessage != null) {
                // Проверяем на признаки constraint violations
                if (causeMessage.toLowerCase().contains("duplicate key") ||
                    causeMessage.toLowerCase().contains("unique constraint") ||
                    causeMessage.toLowerCase().contains("violates unique constraint") ||
                    causeMessage.toLowerCase().contains("duplicate entry") ||
                    causeMessage.toLowerCase().contains("constraint violation") ||
                    causeMessage.toLowerCase().contains("duplicate value") ||
                    causeMessage.toLowerCase().contains("validation failed") || // Bean Validation
                    causeMessage.contains("23505") || // PostgreSQL unique violation
                    causeMessage.contains("23000") || // MySQL integrity constraint violation
                    rootCause.getClass().getSimpleName().contains("ConstraintViolation") ||
                    rootCause.getClass().getSimpleName().contains("IntegrityConstraint") ||
                    rootCause.getClass().getSimpleName().contains("ValidationException")) {
                    
                    log.info("CONSTRAINT CHECK: Detected constraint violation in cause: {}", causeMessage);
                    return true;
                }
            }
            
            rootCause = rootCause.getCause();
        }
        
        // Дополнительная проверка по сообщению исключения
        if (message != null) {
            if (message.toLowerCase().contains("duplicate key") ||
                message.toLowerCase().contains("unique constraint") ||
                message.toLowerCase().contains("constraint violation") ||
                message.toLowerCase().contains("validation failed") || // Bean Validation
                message.toLowerCase().contains("status_marked_rollback") ||
                message.toLowerCase().contains("duplicate entry")) {
                
                log.info("CONSTRAINT CHECK: Detected constraint violation in main message: {}", message);
                return true;
            }
        }
        
        // Проверяем тип исключения
        if (e.getClass().getSimpleName().contains("ConstraintViolation") ||
            e.getClass().getSimpleName().contains("IntegrityConstraint") ||
            e.getClass().getSimpleName().contains("ValidationException") ||
            e.getClass().getSimpleName().contains("SQLException")) {
            
            log.info("CONSTRAINT CHECK: Detected constraint violation by exception type: {}", e.getClass().getSimpleName());
            return true;
        }
        
        log.info("CONSTRAINT CHECK: No constraint violation detected");
        return false;
    }
    
    /**
     * Конвертирует constraint violation в специфичные исключения для HTTP 409
     */
    private RuntimeException convertConstraintViolationToConflict(Exception e, RouteCreateDto dto) {
        String message = e.getMessage();
        if (message == null && e.getCause() != null) {
            message = e.getCause().getMessage();
        }

        if (message == null) {
            return new RuntimeException("Неизвестный конфликт данных маршрута");
        }

        // ДОБАВЛЕНО: Обработка Bean Validation ошибок
        if (message.contains("Validation failed") && message.contains("distance") && message.contains("must be greater than or equal to 2")) {
            return new IllegalArgumentException("Расстояние маршрута должно быть не менее 2");
        }

        // Определяем тип constraint violation для МАРШРУТОВ
        if (message.contains("routes_name") || message.contains("route") && message.contains("name")) {
            // Дублирующееся название маршрута
            if (dto.name() != null) {
                try {
                    // Ищем конфликтующий маршрут
                    List<Route> conflictingRoutes = em.createQuery(
                                    "SELECT r FROM Route r WHERE LOWER(r.name) = LOWER(:name)",
                                    Route.class)
                            .setParameter("name", dto.name().trim())
                            .getResultList();

                    if (!conflictingRoutes.isEmpty()) {
                        Route conflictingRoute = conflictingRoutes.get(0);
                        RouteDto conflictingRouteDto = RouteMapper.toDto(conflictingRoute);
                        log.info("Found conflicting route: ID={}, Name='{}'",
                                conflictingRouteDto.id(), conflictingRouteDto.name());
                        return new RouteNameAlreadyExistsException(dto.name().trim(), conflictingRouteDto);
                    }

                    // Если не нашли, значит constraint violation был по другой причине
                    // или маршрут уже удален/изменен - просто возвращаем исключение без деталей
                    log.warn("Constraint violation for name '{}' but no conflicting route found in DB",
                            dto.name().trim());
                    return new RouteNameAlreadyExistsException(dto.name().trim());

                } catch (Exception ex) {
                    log.error("Error searching for conflicting route: {}", ex.getMessage(), ex);
                    // При ошибке поиска возвращаем базовое исключение
                    return new RouteNameAlreadyExistsException(dto.name().trim());
                }
            }
        }

        // Общий constraint violation для маршрутов
        return new RuntimeException("Конфликт данных маршрута: " + message);
    }
    
    /**
     * Конвертирует constraint violation при обновлении в специфичные исключения для HTTP 409
     */
    private RuntimeException convertUpdateConstraintViolationToConflict(Exception e, RouteUpdateDto dto) {
        String message = e.getMessage();
        if (message == null && e.getCause() != null) {
            message = e.getCause().getMessage();
        }

        if (message == null) {
            return new RuntimeException("Неизвестный конфликт данных при обновлении маршрута");
        }

        // ДОБАВЛЕНО: Обработка Bean Validation ошибок при обновлении
        if (message.contains("Validation failed") && message.contains("distance") && message.contains("must be greater than or equal to 2")) {
            return new IllegalArgumentException("Расстояние маршрута должно быть не менее 2");
        }

        // Определяем тип constraint violation для МАРШРУТОВ при обновлении
        if (message.contains("routes_name") || message.contains("route") && message.contains("name")) {
            // Дублирующееся название маршрута
            if (dto.name() != null) {
                try {
                    // Ищем конфликтующий маршрут
                    List<Route> conflictingRoutes = em.createQuery(
                                    "SELECT r FROM Route r WHERE LOWER(r.name) = LOWER(:name) AND r.id != :excludeId",
                                    Route.class)
                            .setParameter("name", dto.name().trim())
                            .setParameter("excludeId", dto.id())
                            .getResultList();

                    if (!conflictingRoutes.isEmpty()) {
                        Route conflictingRoute = conflictingRoutes.get(0);
                        RouteDto conflictingRouteDto = RouteMapper.toDto(conflictingRoute);
                        log.info("Found conflicting route during update: ID={}, Name='{}'",
                                conflictingRouteDto.id(), conflictingRouteDto.name());
                        return new RouteNameAlreadyExistsException(dto.name().trim(), conflictingRouteDto);
                    }

                    // Если не нашли, значит constraint violation был по другой причине
                    log.warn("Constraint violation for name '{}' during update but no conflicting route found in DB",
                            dto.name().trim());
                    return new RouteNameAlreadyExistsException(dto.name().trim());

                } catch (Exception ex) {
                    log.error("Error searching for conflicting route during update: {}", ex.getMessage(), ex);
                    return new RouteNameAlreadyExistsException(dto.name().trim());
                }
            }
        }

        // Общий constraint violation для обновления маршрутов
        return new RuntimeException("Конфликт данных при обновлении маршрута: " + message);
    }
}