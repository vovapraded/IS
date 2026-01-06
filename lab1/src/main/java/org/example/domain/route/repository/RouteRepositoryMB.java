package org.example.domain.route.repository;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.domain.route.dto.CompositeCursor;
import org.example.domain.route.dto.RouteUpdateDto;
import org.example.domain.route.entity.Route;
import org.example.domain.route.mapper.RouteMapper;

import java.util.List;

@Stateless
public class RouteRepositoryMB {

    @PersistenceContext(unitName = "RoutesPU")
    private EntityManager em;

    public Route findById(Integer id) {
        return em.find(Route.class, id);
    }

    public List<Route> findAll() {
        return em.createQuery("SELECT r FROM Route r ORDER BY r.id", Route.class)
                .getResultList();
    }


    /**
     * Простая offset/limit пагинация (заменяет cursor пагинацию)
     */
    public List<Route> findPaginated(int page, int size, String nameFilter, String sortBy, String sortDirection) {
        StringBuilder jpql = new StringBuilder("SELECT r FROM Route r");
        
        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            jpql.append(" WHERE LOWER(r.name) LIKE LOWER(:nameFilter)");
        }
        
        jpql.append(" ORDER BY ");
        appendSortClause(jpql, sortBy, sortDirection);
        
        var query = em.createQuery(jpql.toString(), Route.class);
        
        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            query.setParameter("nameFilter", "%" + nameFilter.trim() + "%");
        }
        
        return query.setFirstResult(page * size)
                   .setMaxResults(size)
                   .getResultList();
    }

    public long countAll() {
        return em.createQuery("SELECT COUNT(r) FROM Route r", Long.class)
                .getSingleResult();
    }

    public long countWithFilter(String nameFilter) {
        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            return em.createQuery("SELECT COUNT(r) FROM Route r WHERE LOWER(r.name) LIKE LOWER(:nameFilter)", Long.class)
                    .setParameter("nameFilter", "%" + nameFilter.trim() + "%")
                    .getSingleResult();
        } else {
            return countAll();
        }
    }


    public Route save(Route route) {
        if (route.getId() == null) {
            // Новая сущность - persist
            em.persist(route);
            return route;
        } else {
            // Существующая сущность - merge
            return em.merge(route);
        }
    }

    public Route updateFromDto(RouteUpdateDto dto) {
        Route existing = em.find(Route.class, dto.id());
        if (existing == null) {
            throw new IllegalArgumentException("Route not found with id: " + dto.id());
        }

        // Обновляем базовые поля через маппер
        RouteMapper.updateEntityFromDto(existing, dto);
        
        // Обрабатываем связанные объекты через EntityManager
        if (dto.coordinates() != null && dto.coordinates().id() != null) {
            org.example.domain.coordinates.entity.Coordinates coordinates =
                em.find(org.example.domain.coordinates.entity.Coordinates.class, dto.coordinates().id());
            if (coordinates != null) {
                // Обновляем поля координат
                coordinates.setX(dto.coordinates().x());
                coordinates.setY(dto.coordinates().y());
                existing.setCoordinates(coordinates);
            }
        }
        
        if (dto.from() != null && dto.from().id() != null) {
            org.example.domain.location.entity.Location fromLocation =
                em.find(org.example.domain.location.entity.Location.class, dto.from().id());
            if (fromLocation != null) {
                // Обновляем поля локации from
                fromLocation.setX(dto.from().x());
                fromLocation.setY(dto.from().y());
                fromLocation.setName(dto.from().name());
                existing.setFrom(fromLocation);
            }
        }
        
        if (dto.to() != null && dto.to().id() != null) {
            org.example.domain.location.entity.Location toLocation =
                em.find(org.example.domain.location.entity.Location.class, dto.to().id());
            if (toLocation != null) {
                // Обновляем поля локации to
                toLocation.setX(dto.to().x());
                toLocation.setY(dto.to().y());
                toLocation.setName(dto.to().name());
                existing.setTo(toLocation);
            }
        }
        
        // existing уже managed, изменения автоматически сохранятся при commit
        return existing;
    }
    
    public void delete(Route route) {
        if (route == null) {
            throw new IllegalArgumentException("Route cannot be null");
        }
        
        Route managedRoute = em.find(Route.class, route.getId());
        if (managedRoute != null) {
            em.remove(managedRoute);
        }
    }
    
    public void deleteById(Integer id) {
        Route route = em.find(Route.class, id);
        if (route != null) {
            em.remove(route);
        }
    }

    // Специальные операции согласно ТЗ
    
    public Route findRouteWithMaxName() {
        List<Route> results = em.createQuery(
            "SELECT r FROM Route r WHERE r.name = (SELECT MAX(r2.name) FROM Route r2) ORDER BY r.id",
            Route.class
        ).setMaxResults(1).getResultList();
        
        return results.isEmpty() ? null : results.get(0);
    }

    public long countRoutesWithRatingLessThan(Long ratingThreshold) {
        return em.createQuery("SELECT COUNT(r) FROM Route r WHERE r.rating < :threshold", Long.class)
                .setParameter("threshold", ratingThreshold)
                .getSingleResult();
    }

    public List<Route> findRoutesWithRatingGreaterThan(Long ratingThreshold) {
        return em.createQuery("SELECT r FROM Route r WHERE r.rating > :threshold ORDER BY r.rating DESC", Route.class)
                .setParameter("threshold", ratingThreshold)
                .getResultList();
    }

    public List<Route> findRoutesBetweenLocations(String fromLocationName, String toLocationName, String sortBy) {
        StringBuilder jpql = new StringBuilder("SELECT r FROM Route r WHERE 1=1");
        
        if (fromLocationName != null && !fromLocationName.trim().isEmpty()) {
            String fromLocation = fromLocationName.trim();
            
            // Проверяем, является ли это координатами в формате "(x, y)"
            if (fromLocation.startsWith("(") && fromLocation.endsWith(")")) {
                // Извлекаем координаты из формата "(x, y)"
                String coords = fromLocation.substring(1, fromLocation.length() - 1);
                String[] parts = coords.split(",");
                if (parts.length == 2) {
                    try {
                        Double x = Double.parseDouble(parts[0].trim());
                        Double y = Double.parseDouble(parts[1].trim());
                        jpql.append(" AND r.from.x = :fromX AND r.from.y = :fromY");
                    } catch (NumberFormatException e) {
                        // Если не удается распарсить, ищем по названию
                        jpql.append(" AND r.from.name = :fromName");
                    }
                } else {
                    jpql.append(" AND r.from.name = :fromName");
                }
            } else {
                jpql.append(" AND r.from.name = :fromName");
            }
        }
        
        if (toLocationName != null && !toLocationName.trim().isEmpty()) {
            String toLocation = toLocationName.trim();
            
            // Проверяем, является ли это координатами в формате "(x, y)"
            if (toLocation.startsWith("(") && toLocation.endsWith(")")) {
                // Извлекаем координаты из формата "(x, y)"
                String coords = toLocation.substring(1, toLocation.length() - 1);
                String[] parts = coords.split(",");
                if (parts.length == 2) {
                    try {
                        Double x = Double.parseDouble(parts[0].trim());
                        Double y = Double.parseDouble(parts[1].trim());
                        jpql.append(" AND r.to.x = :toX AND r.to.y = :toY");
                    } catch (NumberFormatException e) {
                        // Если не удается распарсить, ищем по названию
                        jpql.append(" AND r.to.name = :toName");
                    }
                } else {
                    jpql.append(" AND r.to.name = :toName");
                }
            } else {
                jpql.append(" AND r.to.name = :toName");
            }
        }
        
        // Добавляем сортировку
        switch (sortBy != null ? sortBy.toLowerCase() : "name") {
            case "distance":
                jpql.append(" ORDER BY r.distance");
                break;
            case "rating":
                jpql.append(" ORDER BY r.rating DESC");
                break;
            case "creation_date":
                jpql.append(" ORDER BY r.creationDate DESC");
                break;
            default:
                jpql.append(" ORDER BY r.name");
        }
        
        var query = em.createQuery(jpql.toString(), Route.class);
        
        // Устанавливаем параметры для FROM локации
        if (fromLocationName != null && !fromLocationName.trim().isEmpty()) {
            String fromLocation = fromLocationName.trim();
            
            if (fromLocation.startsWith("(") && fromLocation.endsWith(")")) {
                String coords = fromLocation.substring(1, fromLocation.length() - 1);
                String[] parts = coords.split(",");
                if (parts.length == 2) {
                    try {
                        Double x = Double.parseDouble(parts[0].trim());
                        Double y = Double.parseDouble(parts[1].trim());
                        query.setParameter("fromX", x);
                        query.setParameter("fromY", y);
                    } catch (NumberFormatException e) {
                        query.setParameter("fromName", fromLocation);
                    }
                } else {
                    query.setParameter("fromName", fromLocation);
                }
            } else {
                query.setParameter("fromName", fromLocation);
            }
        }
        
        // Устанавливаем параметры для TO локации
        if (toLocationName != null && !toLocationName.trim().isEmpty()) {
            String toLocation = toLocationName.trim();
            
            if (toLocation.startsWith("(") && toLocation.endsWith(")")) {
                String coords = toLocation.substring(1, toLocation.length() - 1);
                String[] parts = coords.split(",");
                if (parts.length == 2) {
                    try {
                        Double x = Double.parseDouble(parts[0].trim());
                        Double y = Double.parseDouble(parts[1].trim());
                        query.setParameter("toX", x);
                        query.setParameter("toY", y);
                    } catch (NumberFormatException e) {
                        query.setParameter("toName", toLocation);
                    }
                } else {
                    query.setParameter("toName", toLocation);
                }
            } else {
                query.setParameter("toName", toLocation);
            }
        }
        
        return query.getResultList();
    }
    
    public List<Route> findRoutesBetweenLocations(String fromLocationName, String toLocationName,
                                                 Double fromX, Double fromY, Double toX, Double toY, String sortBy) {
        StringBuilder jpql = new StringBuilder("SELECT r FROM Route r WHERE 1=1");
        
        // Поиск по локации FROM
        if (fromX != null && fromY != null) {
            jpql.append(" AND r.from.x = :fromX AND r.from.y = :fromY");
            if (fromLocationName != null && !fromLocationName.trim().isEmpty()) {
                if ("без названия".equalsIgnoreCase(fromLocationName.trim())) {
                    jpql.append(" AND (r.from.name IS NULL OR r.from.name = '')");
                } else {
                    jpql.append(" AND r.from.name = :fromName");
                }
            }
        } else if (fromLocationName != null && !fromLocationName.trim().isEmpty()) {
            if ("без названия".equalsIgnoreCase(fromLocationName.trim())) {
                jpql.append(" AND (r.from.name IS NULL OR r.from.name = '')");
            } else {
                jpql.append(" AND r.from.name = :fromName");
            }
        }
        
        // Поиск по локации TO
        if (toX != null && toY != null) {
            jpql.append(" AND r.to.x = :toX AND r.to.y = :toY");
            if (toLocationName != null && !toLocationName.trim().isEmpty()) {
                if ("без названия".equalsIgnoreCase(toLocationName.trim())) {
                    jpql.append(" AND (r.to.name IS NULL OR r.to.name = '')");
                } else {
                    jpql.append(" AND r.to.name = :toName");
                }
            }
        } else if (toLocationName != null && !toLocationName.trim().isEmpty()) {
            if ("без названия".equalsIgnoreCase(toLocationName.trim())) {
                jpql.append(" AND (r.to.name IS NULL OR r.to.name = '')");
            } else {
                jpql.append(" AND r.to.name = :toName");
            }
        }
        
        // Добавляем сортировку
        switch (sortBy != null ? sortBy.toLowerCase() : "name") {
            case "distance":
                jpql.append(" ORDER BY r.distance");
                break;
            case "rating":
                jpql.append(" ORDER BY r.rating DESC");
                break;
            case "creation_date":
                jpql.append(" ORDER BY r.creationDate DESC");
                break;
            default:
                jpql.append(" ORDER BY r.name");
        }
        
        var query = em.createQuery(jpql.toString(), Route.class);
        
        // Устанавливаем параметры
        if (fromX != null && fromY != null) {
            query.setParameter("fromX", fromX);
            query.setParameter("fromY", fromY);
        }
        
        if (toX != null && toY != null) {
            query.setParameter("toX", toX);
            query.setParameter("toY", toY);
        }
        
        if (fromLocationName != null && !fromLocationName.trim().isEmpty() &&
            !"без названия".equalsIgnoreCase(fromLocationName.trim())) {
            query.setParameter("fromName", fromLocationName.trim());
        }
        
        if (toLocationName != null && !toLocationName.trim().isEmpty() &&
            !"без названия".equalsIgnoreCase(toLocationName.trim())) {
            query.setParameter("toName", toLocationName.trim());
        }
        
        return query.getResultList();
    }

    public Integer addRouteBetweenLocations(String routeName, float coordX, Double coordY,
                                           Double fromX, double fromY, String fromName,
                                           Double toX, double toY, String toName,
                                           Long distance, Long rating) {
        // Используем новую структуру с отдельными таблицами
        // Этот метод должен использовать сервисы для создания координат и локаций
        // Но для совместимости сделаем простую заглушку
        throw new UnsupportedOperationException("Use RouteService.createRoute() instead for proper entity management");
    }

    // Пагинация (cursor-based с композитными курсорами)
    
    /**
     * Получить первую страницу маршрутов
     */
    public List<Route> findFirstPage(int limit, String nameFilter, String sortBy, String sortDirection) {
        StringBuilder jpql = new StringBuilder("SELECT r FROM Route r");
        
        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            jpql.append(" WHERE LOWER(r.name) LIKE LOWER(:nameFilter)");
        }
        
        jpql.append(" ORDER BY ");
        appendSortClause(jpql, sortBy, sortDirection);
        
        var query = em.createQuery(jpql.toString(), Route.class);
        
        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            query.setParameter("nameFilter", "%" + nameFilter.trim() + "%");
        }
        
        return query.setMaxResults(limit).getResultList();
    }
    
    /**
     * Получить следующую страницу после указанного cursor'а (с композитным курсором)
     */
    public List<Route> findNextPage(CompositeCursor cursor, int limit, String nameFilter) {
        StringBuilder jpql = new StringBuilder("SELECT r FROM Route r WHERE ");
        
        // Условие для cursor (зависит от направления сортировки)
        appendCompositeCursorCondition(jpql, cursor, true);
        
        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            jpql.append(" AND LOWER(r.name) LIKE LOWER(:nameFilter)");
        }
        
        jpql.append(" ORDER BY ");
        appendSortClause(jpql, cursor.sortField(), cursor.sortDirection());
        
        var query = em.createQuery(jpql.toString(), Route.class);
        setCompositeCursorParameters(query, cursor);
        
        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            query.setParameter("nameFilter", "%" + nameFilter.trim() + "%");
        }
        
        return query.setMaxResults(limit).getResultList();
    }
    
    /**
     * Получить предыдущую страницу до указанного cursor'а (с композитным курсором)
     */
    public List<Route> findPrevPage(CompositeCursor cursor, int limit, String nameFilter) {
        StringBuilder jpql = new StringBuilder("SELECT r FROM Route r WHERE ");
        
        // Условие для cursor (обратное направление)
        appendCompositeCursorCondition(jpql, cursor, false);
        
        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            jpql.append(" AND LOWER(r.name) LIKE LOWER(:nameFilter)");
        }
        
        jpql.append(" ORDER BY ");
        // Для предыдущей страницы инвертируем сортировку
        appendSortClause(jpql, cursor.sortField(), invertDirection(cursor.sortDirection()));
        
        var query = em.createQuery(jpql.toString(), Route.class);
        setCompositeCursorParameters(query, cursor);
        
        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            query.setParameter("nameFilter", "%" + nameFilter.trim() + "%");
        }
        
        List<Route> results = query.setMaxResults(limit).getResultList();
        // Переворачиваем результат для правильного порядка
        java.util.Collections.reverse(results);
        return results;
    }
    
    /**
     * Получить маршруты по списку ID (для кандидатов при удалении)
     * Заменяет проблематичный findAll().filter()
     */
    public List<Route> findByCoordinatesIdExcluding(Integer coordinatesId, Integer excludeRouteId) {
        return em.createQuery(
            "SELECT r FROM Route r WHERE r.coordinates.id = :coordId AND r.id != :excludeId",
            Route.class)
            .setParameter("coordId", coordinatesId)
            .setParameter("excludeId", excludeRouteId)
            .getResultList();
    }
    
    /**
     * Получить маршруты использующие указанную локацию (как from или to), исключая указанный маршрут
     */
    public List<Route> findByLocationIdExcluding(Integer locationId, Integer excludeRouteId) {
        return em.createQuery(
            "SELECT r FROM Route r WHERE (r.from.id = :locId OR r.to.id = :locId) AND r.id != :excludeId",
            Route.class)
            .setParameter("locId", locationId)
            .setParameter("excludeId", excludeRouteId)
            .getResultList();
    }
    
    // Методы для проверки уникальности
    
    /**
     * Найти маршрут по имени
     */
    public Route findByName(String name) {
        System.out.println("REPO: Searching for route by name: '" + name + "'");
        List<Route> results = em.createQuery(
            "SELECT r FROM Route r WHERE r.name = :name",
            Route.class)
            .setParameter("name", name)
            .setMaxResults(1)
            .getResultList();
        
        System.out.println("REPO: Found " + results.size() + " routes with name: '" + name + "'");
        Route result = results.isEmpty() ? null : results.get(0);
        if (result != null) {
            System.out.println("REPO: Found existing route - ID: " + result.getId() + ", Name: '" + result.getName() + "'");
        } else {
            System.out.println("REPO: No route found with name: '" + name + "'");
        }
        return result;
    }
    
    /**
     * Найти маршрут по имени, исключая маршрут с указанным ID (для обновления)
     */
    public Route findByNameExcluding(String name, Integer excludeRouteId) {
        List<Route> results = em.createQuery(
            "SELECT r FROM Route r WHERE r.name = :name AND r.id != :excludeId",
            Route.class)
            .setParameter("name", name)
            .setParameter("excludeId", excludeRouteId)
            .setMaxResults(1)
            .getResultList();
        
        return results.isEmpty() ? null : results.get(0);
    }
    
    
    // Вспомогательные методы для пагинации
    
    private void appendSortClause(StringBuilder jpql, String sortBy, String sortDirection) {
        switch (sortBy != null ? sortBy.toLowerCase() : "id") {
            case "name":
                jpql.append("r.name");
                break;
            case "distance":
                jpql.append("r.distance");
                break;
            case "rating":
                jpql.append("r.rating");
                break;
            case "creationdate":
                jpql.append("r.creationDate");
                break;
            default:
                jpql.append("r.id");
        }
        
        if ("desc".equalsIgnoreCase(sortDirection)) {
            jpql.append(" DESC");
        } else {
            jpql.append(" ASC");
        }
        
        // Всегда добавляем ID как вторичную сортировку для стабильности cursor'а
        if (!"id".equals(sortBy)) {
            jpql.append(", r.id ");
            jpql.append("desc".equalsIgnoreCase(sortDirection) ? "DESC" : "ASC");
        }
    }
    
    /**
     * Добавляет условие для композитного курсора (БЕЗ подзапросов!)
     */
    private void appendCompositeCursorCondition(StringBuilder jpql, CompositeCursor cursor, boolean isNext) {
        String operator;
        boolean isDesc = cursor.isDescending();
        
        if (isNext) {
            operator = isDesc ? "<" : ">";
        } else {
            operator = isDesc ? ">" : "<";
        }
        
        switch (cursor.sortField().toLowerCase()) {
            case "name":
                jpql.append("(r.name ").append(operator).append(" :cursorValue");
                jpql.append(" OR (r.name = :cursorValue AND r.id ").append(operator).append(" :cursorId))");
                break;
            case "distance":
                jpql.append("(r.distance ").append(operator).append(" :cursorValue");
                jpql.append(" OR (r.distance = :cursorValue AND r.id ").append(operator).append(" :cursorId))");
                break;
            case "rating":
                jpql.append("(r.rating ").append(operator).append(" :cursorValue");
                jpql.append(" OR (r.rating = :cursorValue AND r.id ").append(operator).append(" :cursorId))");
                break;
            case "creationdate":
                jpql.append("(r.creationDate ").append(operator).append(" :cursorValue");
                jpql.append(" OR (r.creationDate = :cursorValue AND r.id ").append(operator).append(" :cursorId))");
                break;
            default:
                jpql.append("r.id ").append(operator).append(" :cursorId");
        }
    }
    
    /**
     * Устанавливает параметры для композитного курсора
     */
    private void setCompositeCursorParameters(jakarta.persistence.Query query, CompositeCursor cursor) {
        query.setParameter("cursorId", cursor.id());
        
        // Устанавливаем значение поля сортировки, если это не ID
        if (!"id".equals(cursor.sortField().toLowerCase())) {
            switch (cursor.sortField().toLowerCase()) {
                case "name":
                    query.setParameter("cursorValue", cursor.getStringValue());
                    break;
                case "distance":
                case "rating":
                    query.setParameter("cursorValue", cursor.getLongValue());
                    break;
                case "creationdate":
                    query.setParameter("cursorValue", cursor.getZonedDateTimeValue());
                    break;
            }
        }
    }
    
    private String invertDirection(String direction) {
        return "desc".equalsIgnoreCase(direction) ? "asc" : "desc";
    }
}