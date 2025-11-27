package org.example.domain.import_history.mapper;

import org.example.domain.import_history.dto.ImportOperationDto;
import org.example.domain.import_history.entity.ImportOperation;

public class ImportOperationMapper {

    public static ImportOperationDto toDto(ImportOperation entity) {
        if (entity == null) {
            return null;
        }
        
        return new ImportOperationDto(
                entity.getId(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getStatus(),
                entity.getUsername(),
                entity.getFilename(),
                entity.getTotalRecords(),
                entity.getProcessedRecords(),
                entity.getSuccessfulRecords(),
                entity.getErrorMessage()
        );
    }

    public static ImportOperation toEntity(ImportOperationDto dto) {
        if (dto == null) {
            return null;
        }
        
        return ImportOperation.builder()
                .id(dto.id())
                .startTime(dto.startTime())
                .endTime(dto.endTime())
                .status(dto.status())
                .username(dto.username())
                .filename(dto.filename())
                .totalRecords(dto.totalRecords())
                .processedRecords(dto.processedRecords())
                .successfulRecords(dto.successfulRecords())
                .errorMessage(dto.errorMessage())
                .build();
    }
}