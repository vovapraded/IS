package org.example.domain.import_history.dto;

import org.example.domain.import_history.entity.ImportStatus;
import java.time.ZonedDateTime;

public record ImportOperationDto(
        Integer id,
        ZonedDateTime startTime,
        ZonedDateTime endTime,
        ImportStatus status,
        String username,
        String filename,
        Integer totalRecords,
        Integer processedRecords,
        Integer successfulRecords,
        String errorMessage
) {}