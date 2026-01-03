package org.example.domain.import_history.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.coordinates.dto.CoordinatesDto;
import org.example.domain.coordinates.entity.Coordinates;
import org.example.domain.import_history.dto.ImportOperationDto;
import org.example.domain.import_history.dto.ImportRequestDto;
import org.example.domain.import_history.dto.ImportResultDto;
import org.example.domain.import_history.entity.ImportStatus;
import org.example.domain.location.dto.LocationDto;
import org.example.domain.location.entity.Location;
import org.example.domain.route.dto.RouteCreateDto;
import org.example.domain.route.entity.Route;
import org.example.domain.route.service.RouteServiceMB;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Stateless
public class RouteImportServiceMB {

    @Inject
    private ImportOperationServiceMB importOperationService;

    @Inject
    private RouteServiceMB routeService;

    @Inject
    private Validator validator;

    @PersistenceContext(unitName = "RoutesPU")
    private EntityManager em;

    // Новые ограничения уникальности для бизнес-логики
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Zа-яА-Я0-9\\s_-]+$");
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MIN_NAME_LENGTH = 1;
    
    /**
     * Основной метод импорта маршрутов из CSV файла
     */
    @Transactional
    public ImportResultDto importRoutes(ImportRequestDto request) {
        log.info("Starting import process for user {} with file {}", request.username(), request.filename());
        
        List<String> errors = new ArrayList<>();
        List<RouteImportData> validRoutes = new ArrayList<>();
        Integer operationId = null;
        
        try {
            // Парсинг CSV данных
            List<RouteImportData> parsedRoutes = parseCSV(request.fileContent());
            
            // Создание операции импорта
            ImportOperationDto operation = importOperationService.createImportOperation(
                request.username(), 
                request.filename(), 
                parsedRoutes.size()
            );
            operationId = operation.id();
            
            // Валидация каждой записи
            for (int i = 0; i < parsedRoutes.size(); i++) {
                RouteImportData routeData = parsedRoutes.get(i);
                List<String> validationErrors = validateRouteData(routeData, i + 2); // +2 для учета заголовка и нумерации с 1
                
                if (validationErrors.isEmpty()) {
                    // Проверка дополнительных бизнес-правил
                    List<String> businessErrors = validateBusinessRules(routeData, i + 2);
                    if (businessErrors.isEmpty()) {
                        validRoutes.add(routeData);
                    } else {
                        errors.addAll(businessErrors);
                    }
                } else {
                    errors.addAll(validationErrors);
                }
            }
            
            // Подсчитываем количество ошибок связанных с дубликатами
            int duplicateCount = 0;
            int validationErrorCount = 0;
            
            for (String error : errors) {
                if (error.contains("already exists")) {
                    duplicateCount++;
                } else {
                    validationErrorCount++;
                }
            }
            
            // Если есть критические ошибки валидации (не дубликаты), прекращаем импорт
            if (validationErrorCount > 0) {
                String errorMessage = "Validation failed: " + String.join("; ", errors);
                importOperationService.failImportOperation(operationId, errorMessage);
                
                return new ImportResultDto(
                    operationId,
                    ImportStatus.FAILED,
                    parsedRoutes.size(),
                    0,
                    parsedRoutes.size(),
                    errors,
                    "Import failed due to validation errors"
                );
            }
            
            // Если только дубликаты, но нет новых записей для импорта
            if (validRoutes.isEmpty() && duplicateCount > 0) {
                String message = "All " + duplicateCount + " routes already exist in database";
                importOperationService.completeImportOperation(operationId, 0);
                
                return new ImportResultDto(
                    operationId,
                    ImportStatus.SUCCESS,
                    parsedRoutes.size(),
                    0,
                    duplicateCount,
                    errors, // Показываем предупреждения о дубликатах
                    message
                );
            }
            
            // Проверка уникальности имен маршрутов в рамках импорта
            Set<String> uniqueNames = new HashSet<>();
            for (RouteImportData routeData : validRoutes) {
                if (!uniqueNames.add(routeData.name().toLowerCase())) {
                    errors.add("Duplicate route name in import file: " + routeData.name());
                }
            }
            
            if (!errors.isEmpty()) {
                String errorMessage = "Duplicate names found in import file";
                importOperationService.failImportOperation(operationId, errorMessage);
                
                return new ImportResultDto(
                    operationId,
                    ImportStatus.FAILED,
                    parsedRoutes.size(),
                    0,
                    parsedRoutes.size(),
                    errors,
                    errorMessage
                );
            }
            
            // Импорт валидных записей в одной транзакции
            int successfulCount = importValidRoutes(validRoutes);
            
            // Завершение операции импорта
            importOperationService.completeImportOperation(operationId, successfulCount);
            
            String message;
            if (duplicateCount > 0 && successfulCount > 0) {
                message = String.format("Import completed: %d routes imported, %d routes skipped (already exist)",
                                      successfulCount, duplicateCount);
                log.info("Import completed with mixed results. {} routes imported, {} duplicates skipped",
                        successfulCount, duplicateCount);
            } else if (duplicateCount > 0) {
                message = String.format("No new routes imported: all %d routes already exist", duplicateCount);
                log.info("Import completed but no new routes added. {} duplicates found", duplicateCount);
            } else {
                message = "Import completed successfully";
                log.info("Import completed successfully. {} routes imported", successfulCount);
            }
            
            return new ImportResultDto(
                operationId,
                ImportStatus.SUCCESS,
                parsedRoutes.size(),
                successfulCount,
                duplicateCount,
                duplicateCount > 0 ? errors : Collections.emptyList(), // Показываем предупреждения о дубликатах
                message
            );
            
        } catch (Exception e) {
            log.error("Import failed with exception", e);
            
            if (operationId != null) {
                importOperationService.failImportOperation(operationId, e.getMessage());
            }
            
            return new ImportResultDto(
                operationId,
                ImportStatus.FAILED,
                0,
                0,
                0,
                Arrays.asList(e.getMessage()),
                "Import failed due to system error"
            );
        }
    }
    
    /**
     * Парсинг CSV файла
     */
    private List<RouteImportData> parseCSV(String csvContent) throws Exception {
        List<RouteImportData> routes = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }
            
            // Проверка заголовков CSV
            validateCSVHeaders(headerLine);
            
            String line;
            int lineNumber = 2; // Начинаем с 2, так как первая строка - заголовки
            
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // Пропускаем пустые строки
                }
                
                try {
                    RouteImportData routeData = parseCSVLine(line, lineNumber);
                    routes.add(routeData);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Error parsing line " + lineNumber + ": " + e.getMessage());
                }
                
                lineNumber++;
            }
            
            if (routes.isEmpty()) {
                throw new IllegalArgumentException("No valid data rows found in CSV file");
            }
            
        }
        
        return routes;
    }
    
    /**
     * Валидация заголовков CSV файла
     */
    private void validateCSVHeaders(String headerLine) {
        String[] expectedHeaders = {
            "name", "coordinates_x", "coordinates_y", 
            "from_x", "from_y", "from_name",
            "to_x", "to_y", "to_name",
            "distance", "rating"
        };
        
        String[] actualHeaders = headerLine.split(",");
        
        if (actualHeaders.length != expectedHeaders.length) {
            throw new IllegalArgumentException("CSV must have exactly " + expectedHeaders.length + " columns: " + 
                String.join(", ", expectedHeaders));
        }
        
        for (int i = 0; i < expectedHeaders.length; i++) {
            if (!actualHeaders[i].trim().equalsIgnoreCase(expectedHeaders[i])) {
                throw new IllegalArgumentException("Invalid header at column " + (i + 1) + 
                    ". Expected: " + expectedHeaders[i] + ", Found: " + actualHeaders[i].trim());
            }
        }
    }
    
    /**
     * Парсинг одной строки CSV
     */
    private RouteImportData parseCSVLine(String line, int lineNumber) throws Exception {
        String[] parts = line.split(",", -1); // -1 чтобы сохранить пустые поля в конце
        
        if (parts.length != 11) {
            throw new IllegalArgumentException("Line must have exactly 11 columns");
        }
        
        try {
            return new RouteImportData(
                parts[0].trim(),                           // name
                Float.parseFloat(parts[1].trim()),         // coordinates_x
                Double.parseDouble(parts[2].trim()),       // coordinates_y
                Double.parseDouble(parts[3].trim()),       // from_x
                Double.parseDouble(parts[4].trim()),       // from_y  
                parts[5].trim(),                           // from_name (может быть пустым)
                Double.parseDouble(parts[6].trim()),       // to_x
                Double.parseDouble(parts[7].trim()),       // to_y
                parts[8].trim(),                           // to_name (может быть пустым)
                Long.parseLong(parts[9].trim()),          // distance
                Long.parseLong(parts[10].trim())          // rating
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format: " + e.getMessage());
        }
    }
    
    /**
     * Валидация данных маршрута согласно ограничениям предметной области
     */
    private List<String> validateRouteData(RouteImportData routeData, int lineNumber) {
        List<String> errors = new ArrayList<>();
        String linePrefix = "Line " + lineNumber + ": ";
        
        // Валидация названия маршрута
        if (routeData.name() == null || routeData.name().trim().isEmpty()) {
            errors.add(linePrefix + "Route name cannot be empty");
        } else {
            String name = routeData.name().trim();
            if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
                errors.add(linePrefix + "Route name length must be between " + MIN_NAME_LENGTH + " and " + MAX_NAME_LENGTH + " characters");
            }
            if (!NAME_PATTERN.matcher(name).matches()) {
                errors.add(linePrefix + "Route name contains invalid characters. Only letters, numbers, spaces, underscore and dash are allowed");
            }
        }
        
        // Валидация координат Y (ограничение <= 807)
        if (routeData.coordinatesY() == null) {
            errors.add(linePrefix + "Coordinates Y cannot be null");
        } else if (routeData.coordinatesY() > 807) {
            errors.add(linePrefix + "Coordinates Y must be <= 807, found: " + routeData.coordinatesY());
        }
        
        // Валидация локации from X (не null)
        if (routeData.fromX() == null) {
            errors.add(linePrefix + "From location X cannot be null");
        }
        
        // Валидация расстояния (>= 2)
        if (routeData.distance() == null) {
            errors.add(linePrefix + "Distance cannot be null");
        } else if (routeData.distance() < 2) {
            errors.add(linePrefix + "Distance must be >= 2, found: " + routeData.distance());
        }
        
        // Валидация рейтинга (> 0)
        if (routeData.rating() == null) {
            errors.add(linePrefix + "Rating cannot be null");
        } else if (routeData.rating() <= 0) {
            errors.add(linePrefix + "Rating must be > 0, found: " + routeData.rating());
        }
        
        return errors;
    }
    
    /**
     * Валидация дополнительных бизнес-правил
     */
    private List<String> validateBusinessRules(RouteImportData routeData, int lineNumber) {
        List<String> errors = new ArrayList<>();
        String linePrefix = "Line " + lineNumber + ": ";
        
        // Проверка уникальности имени маршрута в базе данных
        List<Route> existingRoutes = em.createQuery(
            "SELECT r FROM Route r WHERE LOWER(r.name) = LOWER(:name)", Route.class)
            .setParameter("name", routeData.name().trim())
            .getResultList();
        
        if (!existingRoutes.isEmpty()) {
            errors.add(linePrefix + "Route with name '" + routeData.name() + "' already exists");
        }
        
        // Дополнительная валидация: from и to локации не должны быть одинаковыми
        if (Objects.equals(routeData.fromX(), routeData.toX()) && 
            Objects.equals(routeData.fromY(), routeData.toY()) &&
            Objects.equals(routeData.fromName(), routeData.toName())) {
            errors.add(linePrefix + "From and To locations cannot be identical");
        }
        
        return errors;
    }
    
    /**
     * Импорт валидных маршрутов в одной транзакции
     */
    private int importValidRoutes(List<RouteImportData> validRoutes) {
        int successCount = 0;
        
        for (RouteImportData routeData : validRoutes) {
            try {
                // Создание DTO для координат
                CoordinatesDto coordinatesDto = new CoordinatesDto(
                    null, 
                    routeData.coordinatesX(), 
                    routeData.coordinatesY(),
                    null, 
                    null
                );
                
                // Создание DTO для from локации
                LocationDto fromDto = new LocationDto(
                    null,
                    routeData.fromX(),
                    routeData.fromY(),
                    routeData.fromName().isEmpty() ? null : routeData.fromName(),
                    null,
                    null
                );
                
                // Создание DTO для to локации
                LocationDto toDto = new LocationDto(
                    null,
                    routeData.toX(),
                    routeData.toY(),
                    routeData.toName().isEmpty() ? null : routeData.toName(),
                    null,
                    null
                );
                
                // Создание DTO для маршрута
                RouteCreateDto routeCreateDto = new RouteCreateDto(
                    routeData.name().trim(),
                    coordinatesDto,
                    fromDto,
                    toDto,
                    routeData.distance(),
                    routeData.rating()
                );
                
                // Создание маршрута через сервис
                routeService.createRoute(routeCreateDto);
                successCount++;
                
                log.debug("Successfully imported route: {}", routeData.name());
                
            } catch (Exception e) {
                log.error("Failed to import route: {} - {}", routeData.name(), e.getMessage());
                // При любой ошибке выбрасываем исключение для отката транзакции
                throw new RuntimeException("Failed to import route '" + routeData.name() + "': " + e.getMessage());
            }
        }
        
        return successCount;
    }
    
    /**
     * Внутренний класс для хранения данных импорта
     */
    private record RouteImportData(
        String name,
        Float coordinatesX,
        Double coordinatesY,
        Double fromX,
        Double fromY,
        String fromName,
        Double toX,
        Double toY,
        String toName,
        Long distance,
        Long rating
    ) {}
}