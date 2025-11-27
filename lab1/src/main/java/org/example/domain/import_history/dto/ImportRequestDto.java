package org.example.domain.import_history.dto;

public record ImportRequestDto(
        String username,
        String filename,
        String fileContent  // Base64 encoded content или raw text content
) {}