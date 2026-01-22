package org.example.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Контекст транзакции для отслеживания операций
 */
public class TransactionContext {
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