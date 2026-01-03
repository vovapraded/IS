package org.example.domain.route.repository;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    public List<Route> findAll(int offset, int limit) {
        return em.createQuery("SELECT r FROM Route r ORDER BY r.id", Route.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    public List<Route> findAllWithFilter(int offset, int limit, String nameFilter, String sortBy, String sortDirection) {
        StringBuilder jpql = new StringBuilder("SELECT r FROM Route r");
        
        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            jpql.append(" WHERE LOWER(r.name) LIKE LOWER(:nameFilter)");
        }
        
        jpql.append(" ORDER BY ");
        
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
        
        var query = em.createQuery(jpql.toString(), Route.class);
        
        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            query.setParameter("nameFilter", "%" + nameFilter.trim() + "%");
        }
        
        return query.setFirstResult(offset)
                   .setMaxResults(limit)
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

        RouteMapper.updateEntityFromDto(existing, dto);
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
}