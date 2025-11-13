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

    // Специальные операции согласно ТЗ (PostgreSQL функции)
    
    @SuppressWarnings("unchecked")
    public Route findRouteWithMaxName() {
        List<Route> results = em.createNativeQuery("SELECT * FROM get_route_with_max_name()", "RouteMapping")
                .getResultList();
        return results.isEmpty() ? null : (Route) results.get(0);
    }

    public long countRoutesWithRatingLessThan(Long ratingThreshold) {
        return ((Number) em.createNativeQuery("SELECT count_routes_with_rating_less_than(?)")
                .setParameter(1, ratingThreshold)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Route> findRoutesWithRatingGreaterThan(Long ratingThreshold) {
        return em.createNativeQuery("SELECT * FROM get_routes_with_rating_greater_than(?)", "RouteMapping")
                .setParameter(1, ratingThreshold)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Route> findRoutesBetweenLocations(String fromLocationName, String toLocationName, String sortBy) {
        return em.createNativeQuery("SELECT * FROM find_routes_between_locations(?, ?, ?)", "RouteMapping")
                .setParameter(1, fromLocationName)
                .setParameter(2, toLocationName)
                .setParameter(3, sortBy != null ? sortBy : "name")
                .getResultList();
    }

    public Integer addRouteBetweenLocations(String routeName, float coordX, Double coordY,
                                           Double fromX, double fromY, String fromName,
                                           Double toX, double toY, String toName,
                                           Long distance, Long rating) {
        return ((Number) em.createNativeQuery("SELECT add_route_between_locations(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .setParameter(1, routeName)
                .setParameter(2, coordX)
                .setParameter(3, coordY)
                .setParameter(4, fromX)
                .setParameter(5, fromY)
                .setParameter(6, fromName)
                .setParameter(7, toX)
                .setParameter(8, toY)
                .setParameter(9, toName)
                .setParameter(10, distance)
                .setParameter(11, rating)
                .getSingleResult()).intValue();
    }
}