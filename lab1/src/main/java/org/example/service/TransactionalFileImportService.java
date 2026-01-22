package org.example.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.import_history.dto.ImportOperationDto;
import org.example.domain.import_history.dto.ImportRequestDto;
import org.example.domain.import_history.dto.ImportResultDto;
import org.example.domain.import_history.entity.ImportStatus;
import org.example.domain.import_history.service.ImportOperationServiceMB;
import org.example.domain.import_history.service.RouteImportService;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Slf4j
@Stateless
public class TransactionalFileImportService {

    @Inject
    private TransactionCoordinator transactionCoordinator;

    @Inject
    private MinIOService minIOService;

    @Inject
    private ImportOperationServiceMB importOperationService;

    @Inject
    private RouteImportService routeImportService;

    /**
     * Выполняет транзакционный импорт с сохранением файла в MinIO
     */
    public ImportResultDto importRoutesWithFileStorage(ImportRequestDto request) {
        log.info("Starting transactional import with file storage for user: {}, file: {}", 
                request.username(), request.filename());

        try {
            return transactionCoordinator.executeTransaction(new FileImportOperation(request));
        } catch (Exception e) {
            log.error("Transactional import failed", e);
            return new ImportResultDto(
                    null,
                    ImportStatus.FAILED,
                    0,
                    0,
                    0,
                    Arrays.asList("System error: " + e.getMessage()),
                    "Import failed due to system error"
            );
        }
    }

    /**
     * Реализация транзакционной операции импорта файла
     */
    private class FileImportOperation implements TransactionalOperation<ImportResultDto> {
        private final ImportRequestDto request;
        private ImportOperationDto operation;
        private String fileKey;

        public FileImportOperation(ImportRequestDto request) {
            this.request = request;
        }

        @Override
        public ImportResultDto prepare(TransactionContext context) throws Exception {
            log.info("Transaction {}: Preparing file import for {}", context.getTransactionId(), request.filename());

            // Шаг 1: Загружаем файл в MinIO
            byte[] fileContent = request.fileContent().getBytes(StandardCharsets.UTF_8);
            String contentType = detectContentType(request.filename());
            
            log.info("Transaction {}: Uploading file to MinIO", context.getTransactionId());
            MinIOService.FileUploadResult uploadResult = minIOService.uploadFile(
                    request.filename(), 
                    contentType, 
                    fileContent
            );
            
            fileKey = uploadResult.fileKey();
            context.addUploadedFile(fileKey);
            log.info("Transaction {}: File uploaded to MinIO with key: {}", context.getTransactionId(), fileKey);

            // Шаг 2: Создаем операцию импорта в БД с информацией о файле
            log.info("Transaction {}: Creating import operation in database", context.getTransactionId());
            operation = importOperationService.createImportOperation(
                    request.username(),
                    request.filename(),
                    null, // totalRecords будет установлено после парсинга
                    fileKey,
                    uploadResult.fileSize(),
                    uploadResult.contentType()
            );
            
            // Добавляем операцию отката для удаления записи из БД
            context.addRollbackOperation(() -> {
                try {
                    if (operation != null && operation.id() != null) {
                        log.info("Rolling back database operation: {}", operation.id());
                        importOperationService.failImportOperation(operation.id(), "Transaction rolled back");
                    }
                } catch (Exception e) {
                    log.error("Failed to rollback database operation", e);
                }
            });

            // Шаг 3: Выполняем импорт данных (в рамках той же транзакции БД)
            log.info("Transaction {}: Starting data import", context.getTransactionId());
            ImportResultDto result = routeImportService.importRoutes(request);
            
            // Обновляем операцию информацией о результате
            if (result.status() == ImportStatus.SUCCESS) {
                importOperationService.completeImportOperation(operation.id(), result.successfulRecords());
            } else {
                importOperationService.failImportOperation(operation.id(), result.message());
            }

            return result;
        }

        @Override
        public void commit(TransactionContext context) throws Exception {
            log.info("Transaction {}: Committing file import operation", context.getTransactionId());
            // В нашем случае коммит уже произошел в фазе prepare
            // JTA транзакция автоматически закоммитится
            log.info("Transaction {}: File import committed successfully", context.getTransactionId());
        }

        @Override
        public void rollback(TransactionContext context) {
            log.warn("Transaction {}: Rolling back file import operation", context.getTransactionId());

            // Удаляем файл из MinIO
            for (String uploadedFileKey : context.getUploadedFiles()) {
                try {
                    log.info("Deleting file from MinIO: {}", uploadedFileKey);
                    minIOService.deleteFile(uploadedFileKey);
                } catch (Exception e) {
                    log.error("Failed to delete file from MinIO during rollback: {}", uploadedFileKey, e);
                }
            }

            // Выполняем другие операции отката
            for (Runnable rollbackOp : context.getRollbackOperations()) {
                try {
                    rollbackOp.run();
                } catch (Exception e) {
                    log.error("Rollback operation failed", e);
                }
            }

            log.info("Transaction {}: File import rollback completed", context.getTransactionId());
        }

        private String detectContentType(String filename) {
            if (filename == null) return "application/octet-stream";
            
            String lowerFilename = filename.toLowerCase();
            if (lowerFilename.endsWith(".csv")) {
                return "text/csv";
            } else if (lowerFilename.endsWith(".txt")) {
                return "text/plain";
            } else if (lowerFilename.endsWith(".json")) {
                return "application/json";
            } else if (lowerFilename.endsWith(".xml")) {
                return "application/xml";
            }
            
            return "application/octet-stream";
        }
    }
}