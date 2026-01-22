package org.example.domain.import_history.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.import_history.dto.ImportOperationDto;
import org.example.domain.import_history.entity.ImportOperation;
import org.example.domain.import_history.entity.ImportStatus;
import org.example.domain.import_history.mapper.ImportOperationMapper;
import org.example.domain.import_history.repository.ImportOperationRepositoryMB;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Stateless
public class ImportOperationServiceMB {

    @Inject
    private ImportOperationRepositoryMB importOperationRepository;

    public ImportOperationDto findById(Integer id) {
        ImportOperation operation = importOperationRepository.findById(id);
        if (operation == null) {
            throw new IllegalArgumentException("Import operation not found with id: " + id);
        }
        return ImportOperationMapper.toDto(operation);
    }

    public List<ImportOperationDto> findByUsername(String username) {
        return importOperationRepository.findByUsername(username).stream()
                .map(ImportOperationMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<ImportOperationDto> findByUsernameWithPagination(String username, int page, int size) {
        int offset = page * size;
        return importOperationRepository.findByUsernameWithPagination(username, offset, size).stream()
                .map(ImportOperationMapper::toDto)
                .collect(Collectors.toList());
    }

    public long countByUsername(String username) {
        return importOperationRepository.countByUsername(username);
    }

    /**
     * Создает новую операцию импорта
     */
    public ImportOperationDto createImportOperation(String username, String filename, Integer totalRecords) {
        log.info("Creating import operation for user {} with file {}", username, filename);
        
        ImportOperation operation = ImportOperation.builder()
                .username(username)
                .filename(filename)
                .totalRecords(totalRecords)
                .processedRecords(0)
                .successfulRecords(0)
                .status(ImportStatus.IN_PROGRESS)
                .startTime(ZonedDateTime.now())
                .build();

        ImportOperation saved = importOperationRepository.save(operation);
        return ImportOperationMapper.toDto(saved);
    }

    /**
     * Создает новую операцию импорта с информацией о файле
     */
    public ImportOperationDto createImportOperation(String username, String filename, Integer totalRecords,
            String fileKey, Long fileSize, String fileContentType) {
        log.info("Creating import operation for user {} with file {} (key: {}, size: {} bytes)",
                username, filename, fileKey, fileSize);
        
        ImportOperation operation = ImportOperation.builder()
                .username(username)
                .filename(filename)
                .totalRecords(totalRecords)
                .processedRecords(0)
                .successfulRecords(0)
                .status(ImportStatus.IN_PROGRESS)
                .startTime(ZonedDateTime.now())
                .fileKey(fileKey)
                .fileSize(fileSize)
                .fileContentType(fileContentType)
                .build();

        ImportOperation saved = importOperationRepository.save(operation);
        return ImportOperationMapper.toDto(saved);
    }

    /**
     * Обновляет информацию о файле в операции импорта
     */
    public ImportOperationDto updateFileInfo(Integer operationId, String fileKey, Long fileSize, String fileContentType) {
        ImportOperation operation = importOperationRepository.findById(operationId);
        if (operation == null) {
            throw new IllegalArgumentException("Import operation not found with id: " + operationId);
        }

        operation.setFileKey(fileKey);
        operation.setFileSize(fileSize);
        operation.setFileContentType(fileContentType);
        
        ImportOperation updated = importOperationRepository.save(operation);
        return ImportOperationMapper.toDto(updated);
    }

    /**
     * Обновляет прогресс операции импорта
     */
    public ImportOperationDto updateProgress(Integer operationId, Integer processedRecords, Integer successfulRecords) {
        ImportOperation operation = importOperationRepository.findById(operationId);
        if (operation == null) {
            throw new IllegalArgumentException("Import operation not found with id: " + operationId);
        }

        operation.setProcessedRecords(processedRecords);
        operation.setSuccessfulRecords(successfulRecords);
        
        ImportOperation updated = importOperationRepository.save(operation);
        return ImportOperationMapper.toDto(updated);
    }

    /**
     * Завершает операцию импорта с успехом
     */
    public ImportOperationDto completeImportOperation(Integer operationId, Integer successfulRecords) {
        log.info("Completing import operation {} with {} successful records", operationId, successfulRecords);
        
        ImportOperation operation = importOperationRepository.findById(operationId);
        if (operation == null) {
            throw new IllegalArgumentException("Import operation not found with id: " + operationId);
        }

        operation.setStatus(ImportStatus.SUCCESS);
        operation.setEndTime(ZonedDateTime.now());
        operation.setSuccessfulRecords(successfulRecords);
        operation.setProcessedRecords(operation.getTotalRecords());

        ImportOperation updated = importOperationRepository.save(operation);
        return ImportOperationMapper.toDto(updated);
    }

    /**
     * Завершает операцию импорта с ошибкой
     */
    public ImportOperationDto failImportOperation(Integer operationId, String errorMessage) {
        log.error("Failing import operation {} with error: {}", operationId, errorMessage);
        
        ImportOperation operation = importOperationRepository.findById(operationId);
        if (operation == null) {
            throw new IllegalArgumentException("Import operation not found with id: " + operationId);
        }

        operation.setStatus(ImportStatus.FAILED);
        operation.setEndTime(ZonedDateTime.now());
        operation.setErrorMessage(errorMessage);

        ImportOperation updated = importOperationRepository.save(operation);
        return ImportOperationMapper.toDto(updated);
    }

    /**
     * Получает операции по статусу
     */
    public List<ImportOperationDto> findByStatus(ImportStatus status) {
        return importOperationRepository.findByStatus(status).stream()
                .map(ImportOperationMapper::toDto)
                .collect(Collectors.toList());
    }
}