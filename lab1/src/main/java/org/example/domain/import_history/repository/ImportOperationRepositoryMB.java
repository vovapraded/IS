package org.example.domain.import_history.repository;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.domain.import_history.entity.ImportOperation;
import org.example.domain.import_history.entity.ImportStatus;

import java.util.List;

@Stateless
public class ImportOperationRepositoryMB {

    @PersistenceContext(unitName = "RoutesPU")
    private EntityManager em;

    public ImportOperation findById(Integer id) {
        return em.find(ImportOperation.class, id);
    }

    public List<ImportOperation> findByUsername(String username) {
        return em.createQuery("SELECT io FROM ImportOperation io WHERE io.username = :username ORDER BY io.startTime DESC", ImportOperation.class)
                .setParameter("username", username)
                .getResultList();
    }

    public List<ImportOperation> findByUsernameWithPagination(String username, int offset, int limit) {
        return em.createQuery("SELECT io FROM ImportOperation io WHERE io.username = :username ORDER BY io.startTime DESC", ImportOperation.class)
                .setParameter("username", username)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    public long countByUsername(String username) {
        return em.createQuery("SELECT COUNT(io) FROM ImportOperation io WHERE io.username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult();
    }

    public ImportOperation save(ImportOperation operation) {
        if (operation.getId() == null) {
            em.persist(operation);
            return operation;
        } else {
            return em.merge(operation);
        }
    }

    public void delete(ImportOperation operation) {
        if (operation != null) {
            ImportOperation managedOperation = em.find(ImportOperation.class, operation.getId());
            if (managedOperation != null) {
                em.remove(managedOperation);
            }
        }
    }

    public void deleteById(Integer id) {
        ImportOperation operation = em.find(ImportOperation.class, id);
        if (operation != null) {
            em.remove(operation);
        }
    }

    public List<ImportOperation> findByStatus(ImportStatus status) {
        return em.createQuery("SELECT io FROM ImportOperation io WHERE io.status = :status ORDER BY io.startTime DESC", ImportOperation.class)
                .setParameter("status", status)
                .getResultList();
    }
}