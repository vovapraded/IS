package org.example.domain.import_history.controller;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.import_history.dto.ImportOperationDto;
import org.example.domain.import_history.dto.ImportRequestDto;
import org.example.domain.import_history.dto.ImportResultDto;
import org.example.domain.import_history.service.ImportOperationServiceMB;
import org.example.domain.import_history.service.RouteImportService;

import java.util.List;

@Path("/api/import")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class ImportResource {

    @Inject
    private RouteImportService routeImportService;

    @Inject
    private ImportOperationServiceMB importOperationService;

    /**
     * Импорт маршрутов из CSV файла
     */
    @POST
    @Path("/routes")
    public Response importRoutes(ImportRequestDto request, @Context HttpServletRequest httpRequest) {
        try {
            log.info("Received import request for file: {} from user: {}", request.filename(), request.username());
            
            // Валидация входных данных
            if (request.username() == null || request.username().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Username is required\"}")
                    .build();
            }
            
            if (request.filename() == null || request.filename().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Filename is required\"}")
                    .build();
            }
            
            if (request.fileContent() == null || request.fileContent().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"File content is required\"}")
                    .build();
            }

            // Выполнение импорта
            ImportResultDto result = routeImportService.importRoutes(request);
            
            if (result.status().name().equals("SUCCESS")) {
                return Response.ok(result).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(result)
                    .build();
            }
            
        } catch (Exception e) {
            log.error("Error during import", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\":\"Internal server error: " + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Получение истории импорта для всех пользователей (только для администратора)
     */
    @GET
    @Path("/history")
    public Response getImportHistory(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("admin") @DefaultValue("false") boolean isAdmin,
            @QueryParam("username") String username) {
        
        try {
            if (isAdmin) {
                // Администратор видит все операции
                List<ImportOperationDto> operations = importOperationService.findWithPagination(page, size);
                long totalCount = importOperationService.countAll();
                
                return Response.ok()
                    .entity(new ImportHistoryResponse(operations, totalCount, page, size))
                    .build();
            } else {
                // Обычный пользователь видит только свои операции
                if (username == null || username.trim().isEmpty()) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"Username is required for non-admin users\"}")
                        .build();
                }
                
                List<ImportOperationDto> operations = importOperationService.findByUsernameWithPagination(username, page, size);
                long totalCount = importOperationService.countByUsername(username);
                
                return Response.ok()
                    .entity(new ImportHistoryResponse(operations, totalCount, page, size))
                    .build();
            }
            
        } catch (Exception e) {
            log.error("Error getting import history", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\":\"Internal server error: " + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Получение истории импорта для конкретного пользователя
     */
    @GET
    @Path("/history/{username}")
    public Response getUserImportHistory(
            @PathParam("username") String username,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        
        try {
            List<ImportOperationDto> operations = importOperationService.findByUsernameWithPagination(username, page, size);
            long totalCount = importOperationService.countByUsername(username);
            
            return Response.ok()
                .entity(new ImportHistoryResponse(operations, totalCount, page, size))
                .build();
            
        } catch (Exception e) {
            log.error("Error getting user import history for user: {}", username, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\":\"Internal server error: " + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Получение детальной информации об операции импорта
     */
    @GET
    @Path("/operations/{operationId}")
    public Response getImportOperation(@PathParam("operationId") Integer operationId) {
        try {
            ImportOperationDto operation = importOperationService.findById(operationId);
            return Response.ok(operation).build();
            
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Import operation not found\"}")
                .build();
                
        } catch (Exception e) {
            log.error("Error getting import operation: {}", operationId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\":\"Internal server error: " + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Получение статистики импорта
     */
    @GET
    @Path("/stats")
    public Response getImportStats(@QueryParam("username") String username) {
        try {
            long totalOperations;
            long successfulOperations;
            long failedOperations;
            
            if (username != null && !username.trim().isEmpty()) {
                // Статистика для конкретного пользователя
                List<ImportOperationDto> userOperations = importOperationService.findByUsername(username);
                totalOperations = userOperations.size();
                successfulOperations = userOperations.stream()
                    .mapToLong(op -> "SUCCESS".equals(op.status().name()) ? 1 : 0)
                    .sum();
                failedOperations = userOperations.stream()
                    .mapToLong(op -> "FAILED".equals(op.status().name()) ? 1 : 0)
                    .sum();
            } else {
                // Общая статистика
                List<ImportOperationDto> allOperations = importOperationService.findAll();
                totalOperations = allOperations.size();
                successfulOperations = allOperations.stream()
                    .mapToLong(op -> "SUCCESS".equals(op.status().name()) ? 1 : 0)
                    .sum();
                failedOperations = allOperations.stream()
                    .mapToLong(op -> "FAILED".equals(op.status().name()) ? 1 : 0)
                    .sum();
            }
            
            return Response.ok()
                .entity(new ImportStatsResponse(totalOperations, successfulOperations, failedOperations))
                .build();
                
        } catch (Exception e) {
            log.error("Error getting import stats", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\":\"Internal server error: " + e.getMessage() + "\"}")
                .build();
        }
    }

    /**
     * Класс ответа для истории импорта с пагинацией
     */
    public record ImportHistoryResponse(
        List<ImportOperationDto> operations,
        long totalCount,
        int currentPage,
        int pageSize
    ) {}

    /**
     * Класс ответа для статистики импорта
     */
    public record ImportStatsResponse(
        long totalOperations,
        long successfulOperations,
        long failedOperations
    ) {}
}