package org.example.domain.coordinates.repository;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.example.domain.coordinates.entity.Coordinates;

import java.util.List;
import java.util.Optional;

@Stateless
public class CoordinatesRepositoryMB {

    @PersistenceContext(unitName = "RoutesPU")
    private EntityManager em;

    public Coordinates findById(Integer id) {
        return em.find(Coordinates.class, id);
    }

    public List<Coordinates> findAll() {
        return em.createQuery("SELECT c FROM Coordinates c ORDER BY c.id", Coordinates.class)
                .getResultList();
    }

    public Optional<Coordinates> findByXAndY(float x, Double y) {
        List<Coordinates> results = em.createQuery(
            "SELECT c FROM Coordinates c WHERE c.x = :x AND c.y = :y",
            Coordinates.class)
            .setParameter("x", x)
            .setParameter("y", y)
            .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Coordinates> findByExample(float x, Double y) {
        StringBuilder jpql = new StringBuilder("SELECT c FROM Coordinates c WHERE 1=1");
        
        if (y != null) {
            jpql.append(" AND c.y = :y");
        }
        
        jpql.append(" AND c.x = :x");
        
        TypedQuery<Coordinates> query = em.createQuery(jpql.toString(), Coordinates.class);
        query.setParameter("x", x);
        
        if (y != null) {
            query.setParameter("y", y);
        }
        
        return query.getResultList();
    }

    public boolean exists(float x, Double y) {
        Long count = em.createQuery(
            "SELECT COUNT(c) FROM Coordinates c " +
            "WHERE c.x = :x AND c.y = :y",
            Long.class)
            .setParameter("x", x)
            .setParameter("y", y)
            .getSingleResult();
        return count > 0;
    }

    public long countUsages(Integer coordinatesId) {
        return em.createQuery(
            "SELECT COUNT(r) FROM Route r WHERE r.coordinates.id = :coordId",
            Long.class)
            .setParameter("coordId", coordinatesId)
            .getSingleResult();
    }
    
    public long countUsagesExcluding(Integer coordinatesId, Integer excludeRouteId) {
        return em.createQuery(
            "SELECT COUNT(r) FROM Route r WHERE r.coordinates.id = :coordId AND r.id != :excludeRouteId",
            Long.class)
            .setParameter("coordId", coordinatesId)
            .setParameter("excludeRouteId", excludeRouteId)
            .getSingleResult();
    }

    public long countUsagesByValues(float x, Double y) {
        return em.createQuery(
            "SELECT COUNT(r) FROM Route r JOIN r.coordinates c " +
            "WHERE c.x = :x AND c.y = :y",
            Long.class)
            .setParameter("x", x)
            .setParameter("y", y)
            .getSingleResult();
    }

    public Coordinates save(Coordinates coordinates) {
        if (coordinates.getId() == null) {
            em.persist(coordinates);
            return coordinates;
        } else {
            return em.merge(coordinates);
        }
    }

    public void delete(Coordinates coordinates) {
        if (coordinates == null) return;
        
        Coordinates managed = em.find(Coordinates.class, coordinates.getId());
        if (managed != null) {
            em.remove(managed);
        }
    }

    public void deleteById(Integer id) {
        Coordinates coordinates = em.find(Coordinates.class, id);
        if (coordinates != null) {
            em.remove(coordinates);
        }
    }
}