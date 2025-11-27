package org.example.domain.import_history.dto;

import org.example.domain.import_history.entity.ImportStatus;

import java.util.List;

public record ImportResultDto(
        Integer operationId,
        ImportStatus status,
        Integer totalRecords,
        Integer successfulRecords,
        Integer failedRecords,
        List<String> errors,
        String message
) {}