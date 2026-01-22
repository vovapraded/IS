package org.example.service;

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