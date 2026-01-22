package org.example.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@Stateless
public class TransactionCoordinator {

    @Inject
    private MinIOService minIOService;

    @PersistenceContext(unitName = "RoutesPU")
    private EntityManager entityManager;

    /**
     * Выполняет двухфазный коммит для операций с файлами и БД
     */
    @Transactional
    public <T> T executeTransaction(TransactionalOperation<T> operation) throws Exception {
        String transactionId = UUID.randomUUID().toString();
        log.info("Starting distributed transaction: {}", transactionId);
        
        TransactionContext context = new TransactionContext(transactionId);
        
        try {
            // Фаза 1: Подготовка всех ресурсов
            log.info("Transaction {}: Phase 1 - Prepare", transactionId);
            T result = operation.prepare(context);
            
            // Фаза 2: Коммит всех ресурсов
            log.info("Transaction {}: Phase 2 - Commit", transactionId);
            operation.commit(context);
            
            log.info("Transaction {} completed successfully", transactionId);
            return result;
            
        } catch (Exception e) {
            log.error("Transaction {} failed, initiating rollback", transactionId, e);
            
            // Откат всех операций
            try {
                operation.rollback(context);
                log.info("Transaction {} rollback completed", transactionId);
            } catch (Exception rollbackException) {
                log.error("Transaction {} rollback failed", transactionId, rollbackException);
                // В реальной системе здесь нужно запустить процесс восстановления
            }
            
            throw e;
        }
    }
}