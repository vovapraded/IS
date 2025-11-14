package org.example.domain.location.repository;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.example.domain.location.entity.Location;

import java.util.List;
import java.util.Optional;

@Stateless
public class LocationRepositoryMB {

    @PersistenceContext(unitName = "RoutesPU")
    private EntityManager em;

    public Location findById(Integer id) {
        return em.find(Location.class, id);
    }

    public List<Location> findAll() {
        return em.createQuery("SELECT l FROM Location l ORDER BY l.id", Location.class)
                .getResultList();
    }

    public Optional<Location> findByXAndYAndName(Double x, double y, String name) {
        StringBuilder jpql = new StringBuilder("SELECT l FROM Location l WHERE l.x = :x AND l.y = :y");
        
        if (name != null) {
            jpql.append(" AND l.name = :name");
        } else {
            jpql.append(" AND l.name IS NULL");
        }
        
        TypedQuery<Location> query = em.createQuery(jpql.toString(), Location.class)
            .setParameter("x", x)
            .setParameter("y", y);
            
        if (name != null) {
            query.setParameter("name", name);
        }
        
        List<Location> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Location> findAllFrom() {
        return em.createQuery(
            "SELECT DISTINCT r.from FROM Route r WHERE r.from IS NOT NULL",
            Location.class).getResultList();
    }

    public List<Location> findAllTo() {
        return em.createQuery(
            "SELECT DISTINCT r.to FROM Route r WHERE r.to IS NOT NULL",
            Location.class).getResultList();
    }

    public List<Location> findByExample(Double x, Double y, String name) {
        StringBuilder jpql = new StringBuilder("SELECT l FROM Location l WHERE 1=1");
        
        if (x != null) {
            jpql.append(" AND l.x = :x");
        }
        if (y != null) {
            jpql.append(" AND l.y = :y");
        }
        if (name != null && !name.trim().isEmpty()) {
            jpql.append(" AND LOWER(l.name) LIKE LOWER(:name)");
        }
        
        TypedQuery<Location> query = em.createQuery(jpql.toString(), Location.class);
        
        if (x != null) {
            query.setParameter("x", x);
        }
        if (y != null) {
            query.setParameter("y", y);
        }
        if (name != null && !name.trim().isEmpty()) {
            query.setParameter("name", "%" + name.trim() + "%");
        }
        
        return query.getResultList();
    }

    public boolean exists(Double x, double y, String name) {
        StringBuilder jpql = new StringBuilder("SELECT COUNT(l) FROM Location l WHERE l.x = :x AND l.y = :y");
        
        if (name != null) {
            jpql.append(" AND l.name = :name");
        } else {
            jpql.append(" AND l.name IS NULL");
        }
        
        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class)
            .setParameter("x", x)
            .setParameter("y", y);
            
        if (name != null) {
            query.setParameter("name", name);
        }
        
        return query.getSingleResult() > 0;
    }

    public long countUsages(Integer locationId) {
        // Считаем количество уникальных маршрутов, которые используют данную локацию
        return em.createQuery(
            "SELECT COUNT(DISTINCT r) FROM Route r WHERE r.from.id = :locId OR r.to.id = :locId",
            Long.class)
            .setParameter("locId", locationId)
            .getSingleResult();
    }

    public long countUsagesExcluding(Integer locationId, Integer excludeRouteId) {
        // Считаем количество уникальных маршрутов, которые используют данную локацию
        return em.createQuery(
            "SELECT COUNT(DISTINCT r) FROM Route r WHERE (r.from.id = :locId OR r.to.id = :locId) AND r.id != :excludeRouteId",
            Long.class)
            .setParameter("locId", locationId)
            .setParameter("excludeRouteId", excludeRouteId)
            .getSingleResult();
    }

    public long countUsagesByValues(Double x, double y, String name) {
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT COUNT(r) FROM Route r ")
            .append("WHERE (r.from.x = :x AND r.from.y = :y")
            .append(name != null ? " AND r.from.name = :name" : " AND r.from.name IS NULL")
            .append(") OR (r.to.x = :x AND r.to.y = :y")
            .append(name != null ? " AND r.to.name = :name" : " AND r.to.name IS NULL")
            .append(")");

        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class)
            .setParameter("x", x)
            .setParameter("y", y);
            
        if (name != null) {
            query.setParameter("name", name);
        }
        
        return query.getSingleResult();
    }

    public List<String> findDistinctNames() {
        return em.createQuery(
            "SELECT DISTINCT l.name FROM Location l WHERE l.name IS NOT NULL ORDER BY l.name",
            String.class).getResultList();
    }

    public Location save(Location location) {
        if (location.getId() == null) {
            em.persist(location);
            return location;
        } else {
            return em.merge(location);
        }
    }

    public void delete(Location location) {
        if (location == null) return;
        
        Location managed = em.find(Location.class, location.getId());
        if (managed != null) {
            em.remove(managed);
        }
    }

    public void deleteById(Integer id) {
        Location location = em.find(Location.class, id);
        if (location != null) {
            em.remove(location);
        }
    }
}