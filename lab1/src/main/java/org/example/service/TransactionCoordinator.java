package org.example.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
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

    /**
     * Контекст транзакции для отслеживания операций
     */
    public static class TransactionContext {
        private final String transactionId;
        private final List<String> uploadedFiles;
        private final List<Runnable> rollbackOperations;

        public TransactionContext(String transactionId) {
            this.transactionId = transactionId;
            this.uploadedFiles = new ArrayList<>();
            this.rollbackOperations = new ArrayList<>();
        }

        public String getTransactionId() {
            return transactionId;
        }

        public void addUploadedFile(String fileKey) {
            uploadedFiles.add(fileKey);
        }

        public List<String> getUploadedFiles() {
            return uploadedFiles;
        }

        public void addRollbackOperation(Runnable operation) {
            rollbackOperations.add(operation);
        }

        public List<Runnable> getRollbackOperations() {
            return rollbackOperations;
        }
    }

    /**
     * Интерфейс для транзакционных операций
     */
    public interface TransactionalOperation<T> {
        /**
         * Фаза 1: Подготовка операции
         * @param context контекст транзакции
         * @return результат операции
         */
        T prepare(TransactionContext context) throws Exception;

        /**
         * Фаза 2: Подтверждение операции
         * @param context контекст транзакции
         */
        void commit(TransactionContext context) throws Exception;

        /**
         * Откат операции в случае ошибки
         * @param context контекст транзакции
         */
        void rollback(TransactionContext context);
    }
}