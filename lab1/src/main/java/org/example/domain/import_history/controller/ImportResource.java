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

@Path("/import")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class ImportResource {

    @Inject
    private RouteImportService routeImportService;

    @Inject
    private ImportOperationServiceMB importOperationService;

    @Inject
    private org.example.service.TransactionalFileImportService transactionalFileImportService;

    @Inject
    private org.example.service.MinIOService minIOService;

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

            // Выполнение транзакционного импорта с сохранением файла
            ImportResultDto result = transactionalFileImportService.importRoutesWithFileStorage(request);
            
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
     * Получение истории импорта для конкретного пользователя
     */
    @GET
    @Path("/history")
    public Response getImportHistory(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("username") String username) {
        
        try {
            // Пользователь видит только свои операции
            if (username == null || username.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Username is required\"}")
                    .build();
            }
            
            List<ImportOperationDto> operations = importOperationService.findByUsernameWithPagination(username, page, size);
            long totalCount = importOperationService.countByUsername(username);
            
            return Response.ok()
                .entity(new ImportHistoryResponse(operations, totalCount, page, size))
                .build();
            
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
            // Статистика только для указанного пользователя
            if (username == null || username.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Username is required\"}")
                    .build();
            }
            
            List<ImportOperationDto> userOperations = importOperationService.findByUsername(username);
            long totalOperations = userOperations.size();
            long successfulOperations = userOperations.stream()
                .mapToLong(op -> "SUCCESS".equals(op.status().name()) ? 1 : 0)
                .sum();
            long failedOperations = userOperations.stream()
                .mapToLong(op -> "FAILED".equals(op.status().name()) ? 1 : 0)
                .sum();
            
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
     * Скачивание файла операции импорта
     */
    @GET
    @Path("/operations/{operationId}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadImportFile(@PathParam("operationId") Integer operationId) {
        try {
            // Получаем информацию об операции импорта
            ImportOperationDto operation = importOperationService.findById(operationId);
            
            if (operation.fileKey() == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"No file associated with this import operation\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
            }
            
            // Скачиваем файл из MinIO
            org.example.service.MinIOService.FileDownloadResult fileResult =
                minIOService.downloadFile(operation.fileKey());
            
            // Определяем имя файла для скачивания
            String downloadFilename = operation.filename() != null ? operation.filename() : "import_file.csv";
            
            return Response.ok(fileResult.content())
                    .header("Content-Disposition", "attachment; filename=\"" + downloadFilename + "\"")
                    .header("Content-Type", fileResult.contentType() != null ?
                            fileResult.contentType() : MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Length", String.valueOf(fileResult.size()))
                    .build();
                    
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Import operation not found\"}")
                .type(MediaType.APPLICATION_JSON)
                .build();
                
        } catch (Exception e) {
            log.error("Error downloading file for import operation: {}", operationId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\":\"Failed to download file: " + e.getMessage() + "\"}")
                .type(MediaType.APPLICATION_JSON)
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