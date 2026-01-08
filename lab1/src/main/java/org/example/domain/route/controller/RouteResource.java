package org.example.domain.route.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.example.domain.route.dto.*;
import org.example.domain.route.service.RouteServiceMB;
import org.example.domain.coordinates.dto.CoordinatesDto;
import org.example.domain.location.dto.LocationDto;
import org.example.domain.import_history.service.RouteImportServiceMB;
import org.example.domain.import_history.dto.ImportRequestDto;
import org.example.domain.import_history.dto.ImportResultDto;
import org.example.exception.ValidationException;
import org.example.exception.RouteNameAlreadyExistsException;
import org.example.exception.RouteZeroDistanceException;
import java.util.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/routes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Routes", description = "API для управления маршрутами")
public class RouteResource {

    private static final Logger log = Logger.getLogger(RouteResource.class.getName());

    @Inject
    private RouteServiceMB routeService;
    
    @Inject
    private RouteImportServiceMB routeImportService;

    @GET
    @Operation(summary = "Получить все маршруты", description = "Возвращает список всех доступных маршрутов")
    @ApiResponse(responseCode = "200", description = "Список маршрутов успешно получен")
    public List<RouteDto> getAllRoutes() {
        return routeService.findAll();
    }

    @GET
    @Path("/paginated")
    @Operation(summary = "Получить маршруты с пагинацией", description = "Возвращает маршруты с пагинацией и фильтрацией")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Маршруты получены успешно"),
        @ApiResponse(responseCode = "400", description = "Неверные параметры пагинации")
    })
    public Response getPaginatedRoutes(
            @Parameter(description = "Номер страницы") @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Размер страницы") @QueryParam("size") @DefaultValue("10") int size,
            @Parameter(description = "Фильтр по имени") @QueryParam("nameFilter") String nameFilter,
            @Parameter(description = "Поле сортировки") @QueryParam("sortBy") @DefaultValue("id") String sortBy,
            @Parameter(description = "Направление сортировки") @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection) {
        
        try {
            List<RouteDto> routes = routeService.findPaginated(page, size, nameFilter, sortBy, sortDirection);
            long totalElements = routeService.countWithFilter(nameFilter);
            int totalPages = (int) Math.ceil((double) totalElements / size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", routes);
            response.put("page", page);
            response.put("size", size);
            response.put("totalElements", totalElements);
            response.put("totalPages", totalPages);
            response.put("first", page == 0);
            response.put("last", page >= totalPages - 1);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            log.severe("Pagination error: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Pagination system error: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Получить маршрут по ID", description = "Возвращает конкретный маршрут по его идентификатору")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Маршрут найден"),
        @ApiResponse(responseCode = "404", description = "Маршрут не найден")
    })
    public Response getById(@Parameter(description = "ID маршрута") @PathParam("id") Integer id) {
        try {
            RouteDto route = routeService.findById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("route", route);
            return Response.ok(response).build();
        } catch (jakarta.ejb.EJBException e) {
            // EJB контейнер оборачивает исключения из сервиса в EJBException
            log.info("EJBException during route retrieval, checking root cause: " + e.getMessage());
            Throwable rootCause = e.getCause();
            
            if (rootCause instanceof IllegalArgumentException) {
                IllegalArgumentException argEx = (IllegalArgumentException) rootCause;
                if (argEx.getMessage() != null && argEx.getMessage().toLowerCase().contains("not found")) {
                    log.warning("Route not found (from EJBException): " + argEx.getMessage());
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", argEx.getMessage(), "error_type", "NOT_FOUND"))
                            .build();
                }
                log.warning("Invalid argument for route retrieval (from EJBException): " + argEx.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", argEx.getMessage(), "error_type", "INVALID_ARGUMENT"))
                        .build();
            } else {
                log.severe("EJBException with unexpected cause during route retrieval: " + e.getMessage());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of("error", "Не удалось получить маршрут", "error_type", "INTERNAL_ERROR"))
                        .build();
            }
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("not found")) {
                log.warning("Route not found: " + e.getMessage());
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", e.getMessage(), "error_type", "NOT_FOUND"))
                        .build();
            }
            log.warning("Invalid argument for route retrieval: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage(), "error_type", "INVALID_ARGUMENT"))
                    .build();
        } catch (Exception e) {
            log.severe("Unexpected error during route retrieval: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Не удалось получить маршрут", "error_type", "INTERNAL_ERROR"))
                    .build();
        }
    }

    @POST
    @Operation(summary = "Создать новый маршрут", description = "Создает новый маршрут в системе")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Маршрут создан успешно"),
        @ApiResponse(responseCode = "409", description = "Конфликт - маршрут с таким именем уже существует или нулевое расстояние"),
        @ApiResponse(responseCode = "400", description = "Неверные данные для создания")
    })
    public Response create(@Parameter(description = "Данные для создания маршрута") RouteCreateDto dto) {
        log.info("CONTROLLER: Received request to create route: " + dto);
        
        try {
            RouteDto created = routeService.createRoute(dto);
            log.info("CONTROLLER: Route created successfully: " + created.id());
            Map<String, Object> response = new HashMap<>();
            response.put("route", created);
            return Response.status(Response.Status.CREATED).entity(response).build();
        } catch (jakarta.ejb.EJBException e) {
            // EJB контейнер оборачивает исключения из сервиса в EJBException
            log.info("EJBException during route creation, checking root cause: " + e.getMessage());
            Throwable rootCause = e.getCause();
            
            if (rootCause instanceof RouteNameAlreadyExistsException) {
                RouteNameAlreadyExistsException nameEx = (RouteNameAlreadyExistsException) rootCause;
                log.warning("CONTROLLER: Route name already exists (from EJBException): " + nameEx.getMessage());
                Map<String, Object> response = new HashMap<>();
                response.put("error_type", "DUPLICATE_NAME");
                response.put("error", nameEx.getMessage());
                if (nameEx.getConflictingRoute() != null) {
                    response.put("route", nameEx.getConflictingRoute());
                }
                return Response.status(Response.Status.CONFLICT)
                        .entity(response)
                        .build();
            } else if (rootCause instanceof RouteZeroDistanceException) {
                RouteZeroDistanceException zeroEx = (RouteZeroDistanceException) rootCause;
                log.warning("CONTROLLER: Zero distance route validation failed (from EJBException): " + zeroEx.getMessage());
                Map<String, Object> response = new HashMap<>();
                response.put("error_type", "ZERO_DISTANCE_ROUTE");
                response.put("error", zeroEx.getMessage());
                response.put("fromX", zeroEx.getFromX());
                response.put("fromY", zeroEx.getFromY());
                response.put("toX", zeroEx.getToX());
                response.put("toY", zeroEx.getToY());
                return Response.status(Response.Status.CONFLICT)
                        .entity(response)
                        .build();
            } else if (rootCause instanceof ValidationException) {
                ValidationException valEx = (ValidationException) rootCause;
                log.warning("CONTROLLER: Validation error during route creation (from EJBException): " + valEx.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", valEx.getMessage(), "error_type", "VALIDATION_ERROR"))
                        .build();
            } else if (rootCause instanceof IllegalArgumentException) {
                IllegalArgumentException argEx = (IllegalArgumentException) rootCause;
                log.warning("CONTROLLER: IllegalArgumentException during creation (from EJBException): " + argEx.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", argEx.getMessage(), "error_type", "INVALID_ARGUMENT"))
                        .build();
            } else {
                log.severe("EJBException with unexpected cause during route creation: " + e.getMessage());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of("error", "Ошибка выполнения при создании маршрута: " + e.getMessage(), "error_type", "INTERNAL_ERROR"))
                        .build();
            }
        } catch (RouteNameAlreadyExistsException e) {
            log.warning("CONTROLLER: Route name already exists: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error_type", "DUPLICATE_NAME");
            response.put("error", e.getMessage());
            if (e.getConflictingRoute() != null) {
                response.put("route", e.getConflictingRoute());
            }
            return Response.status(Response.Status.CONFLICT)
                    .entity(response)
                    .build();
        } catch (RouteZeroDistanceException e) {
            log.warning("CONTROLLER: Zero distance route validation failed: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error_type", "ZERO_DISTANCE_ROUTE");
            response.put("error", e.getMessage());
            response.put("fromX", e.getFromX());
            response.put("fromY", e.getFromY());
            response.put("toX", e.getToX());
            response.put("toY", e.getToY());
            return Response.status(Response.Status.CONFLICT)
                    .entity(response)
                    .build();
        } catch (ValidationException e) {
            log.warning("CONTROLLER: Validation error during route creation: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage(), "error_type", "VALIDATION_ERROR"))
                    .build();
        } catch (RuntimeException e) {
            log.severe("CONTROLLER: Runtime error during route creation: " + e.getMessage());
            
            // Проверяем, является ли причина исключения нашим кастомным исключением
            Throwable rootCause = e;
            while (rootCause != null) {
                if (rootCause instanceof RouteNameAlreadyExistsException) {
                    RouteNameAlreadyExistsException nameEx = (RouteNameAlreadyExistsException) rootCause;
                    Map<String, Object> response = new HashMap<>();
                    response.put("error_type", "DUPLICATE_NAME");
                    response.put("error", nameEx.getMessage());
                    if (nameEx.getConflictingRoute() != null) {
                        response.put("route", nameEx.getConflictingRoute());
                    }
                    return Response.status(Response.Status.CONFLICT)
                            .entity(response)
                            .build();
                } else if (rootCause instanceof RouteZeroDistanceException) {
                    RouteZeroDistanceException zeroEx = (RouteZeroDistanceException) rootCause;
                    Map<String, Object> response = new HashMap<>();
                    response.put("error_type", "ZERO_DISTANCE_ROUTE");
                    response.put("error", zeroEx.getMessage());
                    response.put("fromX", zeroEx.getFromX());
                    response.put("fromY", zeroEx.getFromY());
                    response.put("toX", zeroEx.getToX());
                    response.put("toY", zeroEx.getToY());
                    return Response.status(Response.Status.CONFLICT)
                            .entity(response)
                            .build();
                } else if (rootCause instanceof IllegalArgumentException) {
                    // ДОБАВЛЕНО: Обработка IllegalArgumentException в RuntimeException для создания
                    IllegalArgumentException argEx = (IllegalArgumentException) rootCause;
                    log.warning("CONTROLLER: IllegalArgumentException wrapped in RuntimeException during creation: " + argEx.getMessage());
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", argEx.getMessage(), "error_type", "INVALID_ARGUMENT"))
                            .build();
                }
                rootCause = rootCause.getCause();
            }
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Ошибка выполнения при создании маршрута: " + e.getMessage(), "error_type", "RUNTIME_ERROR"))
                    .build();
        } catch (Exception e) {
            log.severe("CONTROLLER: Unexpected error during route creation: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Не удалось сохранить маршрут. Попробуйте еще раз.", "error_type", "INTERNAL_ERROR"))
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Обновить маршрут", description = "Обновляет существующий маршрут")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Маршрут обновлен успешно"),
        @ApiResponse(responseCode = "404", description = "Маршрут не найден"),
        @ApiResponse(responseCode = "409", description = "Конфликт при обновлении")
    })
    public Response update(
            @Parameter(description = "ID маршрута") @PathParam("id") Integer id,
            @Parameter(description = "Данные для обновления") RouteUpdateDto dto) {
        log.info("Received request to update route " + id + ": " + dto);
        if (!dto.id().equals(id)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "ID в URL и в теле запроса не совпадают"))
                    .build();
        }
        
        try {
            RouteDto updated = routeService.updateRoute(dto);
            log.info("Route updated successfully: " + updated.id());
            Map<String, Object> response = new HashMap<>();
            response.put("route", updated);
            return Response.ok(response).build();
        } catch (jakarta.ejb.EJBException e) {
            // EJB контейнер оборачивает исключения из сервиса в EJBException
            log.info("EJBException during route update, checking root cause: " + e.getMessage());
            Throwable rootCause = e.getCause();
            
            if (rootCause instanceof RouteNameAlreadyExistsException) {
                RouteNameAlreadyExistsException nameEx = (RouteNameAlreadyExistsException) rootCause;
                log.warning("Route name already exists on update (from EJBException): " + nameEx.getMessage());
                Map<String, Object> response = new HashMap<>();
                response.put("error_type", "DUPLICATE_NAME");
                response.put("error", nameEx.getMessage());
                if (nameEx.getConflictingRoute() != null) {
                    response.put("route", nameEx.getConflictingRoute());
                }
                return Response.status(Response.Status.CONFLICT)
                        .entity(response)
                        .build();
            } else if (rootCause instanceof RouteZeroDistanceException) {
                RouteZeroDistanceException zeroEx = (RouteZeroDistanceException) rootCause;
                log.warning("Zero distance route validation failed on update (from EJBException): " + zeroEx.getMessage());
                Map<String, Object> response = new HashMap<>();
                response.put("error_type", "ZERO_DISTANCE_ROUTE");
                response.put("error", zeroEx.getMessage());
                response.put("fromX", zeroEx.getFromX());
                response.put("fromY", zeroEx.getFromY());
                response.put("toX", zeroEx.getToX());
                response.put("toY", zeroEx.getToY());
                return Response.status(Response.Status.CONFLICT)
                        .entity(response)
                        .build();
            } else if (rootCause instanceof ValidationException) {
                ValidationException valEx = (ValidationException) rootCause;
                log.warning("Validation error during route update (from EJBException): " + valEx.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", valEx.getMessage(), "error_type", "VALIDATION_ERROR"))
                        .build();
            } else if (rootCause instanceof IllegalArgumentException) {
                IllegalArgumentException argEx = (IllegalArgumentException) rootCause;
                if (argEx.getMessage() != null && argEx.getMessage().toLowerCase().contains("not found")) {
                    log.warning("Route not found during update (from EJBException): " + argEx.getMessage());
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", argEx.getMessage(), "error_type", "NOT_FOUND"))
                            .build();
                }
                log.warning("Invalid argument during route update (from EJBException): " + argEx.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", argEx.getMessage(), "error_type", "INVALID_ARGUMENT"))
                        .build();
            } else {
                log.severe("EJBException with unexpected cause during route update: " + e.getMessage());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of("error", "Не удалось обновить маршрут: " + e.getMessage(), "error_type", "INTERNAL_ERROR"))
                        .build();
            }
        } catch (RouteNameAlreadyExistsException e) {
            log.warning("Route name already exists on update: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error_type", "DUPLICATE_NAME");
            response.put("error", e.getMessage());
            if (e.getConflictingRoute() != null) {
                response.put("route", e.getConflictingRoute());
            }
            return Response.status(Response.Status.CONFLICT)
                    .entity(response)
                    .build();
        } catch (RouteZeroDistanceException e) {
            log.warning("Zero distance route validation failed on update: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error_type", "ZERO_DISTANCE_ROUTE");
            response.put("error", e.getMessage());
            response.put("fromX", e.getFromX());
            response.put("fromY", e.getFromY());
            response.put("toX", e.getToX());
            response.put("toY", e.getToY());
            return Response.status(Response.Status.CONFLICT)
                    .entity(response)
                    .build();
        } catch (ValidationException e) {
            log.warning("Validation error during route update: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage(), "error_type", "VALIDATION_ERROR"))
                    .build();
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("not found")) {
                log.warning("Route not found during update: " + e.getMessage());
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", e.getMessage(), "error_type", "NOT_FOUND"))
                        .build();
            }
            log.warning("Invalid argument during route update: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage(), "error_type", "INVALID_ARGUMENT"))
                    .build();
        } catch (RuntimeException e) {
            log.severe("CONTROLLER: Runtime error during route update: " + e.getMessage());
            
            // Проверяем, является ли причина исключения нашим кастомным исключением
            Throwable rootCause = e;
            while (rootCause != null) {
                if (rootCause instanceof RouteNameAlreadyExistsException) {
                    RouteNameAlreadyExistsException nameEx = (RouteNameAlreadyExistsException) rootCause;
                    Map<String, Object> response = new HashMap<>();
                    response.put("error_type", "DUPLICATE_NAME");
                    response.put("error", nameEx.getMessage());
                    if (nameEx.getConflictingRoute() != null) {
                        response.put("route", nameEx.getConflictingRoute());
                    }
                    return Response.status(Response.Status.CONFLICT)
                            .entity(response)
                            .build();
                } else if (rootCause instanceof RouteZeroDistanceException) {
                    RouteZeroDistanceException zeroEx = (RouteZeroDistanceException) rootCause;
                    Map<String, Object> response = new HashMap<>();
                    response.put("error_type", "ZERO_DISTANCE_ROUTE");
                    response.put("error", zeroEx.getMessage());
                    response.put("fromX", zeroEx.getFromX());
                    response.put("fromY", zeroEx.getFromY());
                    response.put("toX", zeroEx.getToX());
                    response.put("toY", zeroEx.getToY());
                    return Response.status(Response.Status.CONFLICT)
                            .entity(response)
                            .build();
                } else if (rootCause instanceof IllegalArgumentException) {
                    // ДОБАВЛЕНО: Обработка IllegalArgumentException в RuntimeException
                    IllegalArgumentException argEx = (IllegalArgumentException) rootCause;
                    log.warning("CONTROLLER: IllegalArgumentException wrapped in RuntimeException: " + argEx.getMessage());
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", argEx.getMessage(), "error_type", "INVALID_ARGUMENT"))
                            .build();
                }
                rootCause = rootCause.getCause();
            }
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Ошибка выполнения при обновлении маршрута: " + e.getMessage(), "error_type", "RUNTIME_ERROR"))
                    .build();
        } catch (Exception e) {
            log.severe("CONTROLLER: Unexpected error during route update: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Не удалось обновить маршрут. Попробуйте еще раз.", "error_type", "INTERNAL_ERROR"))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Удалить маршрут", description = "Удаляет маршрут из системы с возможностью перепривязки зависимостей")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Маршрут удален успешно"),
        @ApiResponse(responseCode = "404", description = "Маршрут не найден"),
        @ApiResponse(responseCode = "400", description = "Некорректные параметры (например, неверный target route ID)")
    })
    public Response delete(
            @Parameter(description = "ID маршрута") @PathParam("id") Integer id,
            @Parameter(description = "ID маршрута для перепривязки координат") @QueryParam("coordinatesTargetRouteId") Integer coordinatesTargetRouteId,
            @Parameter(description = "ID маршрута для перепривязки начальной локации") @QueryParam("fromLocationTargetRouteId") Integer fromLocationTargetRouteId,
            @Parameter(description = "ID маршрута для перепривязки конечной локации") @QueryParam("toLocationTargetRouteId") Integer toLocationTargetRouteId,
            @Parameter(description = "ID общего целевого маршрута") @QueryParam("targetRouteId") Integer targetRouteId) {
        log.info("Received request to delete route " + id);
        
        try {
            if (targetRouteId != null) {
                routeService.deleteWithRebinding(id, targetRouteId, targetRouteId, targetRouteId);
            } else if (coordinatesTargetRouteId != null || fromLocationTargetRouteId != null || toLocationTargetRouteId != null) {
                routeService.deleteWithRebinding(id, coordinatesTargetRouteId, fromLocationTargetRouteId, toLocationTargetRouteId);
            } else {
                routeService.delete(id);
            }
            log.info("Route " + id + " deleted successfully");
            return Response.noContent().build();
        } catch (jakarta.ejb.EJBException e) {
            // EJB контейнер оборачивает исключения из сервиса в EJBException
            log.info("EJBException during route deletion, checking root cause: " + e.getMessage());
            Throwable rootCause = e.getCause();
            
            if (rootCause instanceof IllegalArgumentException) {
                IllegalArgumentException argEx = (IllegalArgumentException) rootCause;
                String message = argEx.getMessage();
                if (message != null) {
                    // Определяем конкретный тип ошибки по сообщению
                    if (message.toLowerCase().contains("route not found with id: " + id)) {
                        // Это ошибка именно для удаляемого маршрута
                        log.info("Route " + id + " not found during deletion (from EJBException)");
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", "Маршрут не найден", "error_type", "NOT_FOUND"))
                                .build();
                    } else if (message.toLowerCase().contains("target route not found") ||
                              message.toLowerCase().contains("coordinates target route not found") ||
                              message.toLowerCase().contains("from location target route not found") ||
                              message.toLowerCase().contains("to location target route not found")) {
                        // Это ошибка в target route для rebinding
                        log.warning("Target route not found during deletion with rebinding (from EJBException): " + message);
                        return Response.status(Response.Status.BAD_REQUEST)
                                .entity(Map.of("error", "Указанный целевой маршрут не найден: " + message, "error_type", "TARGET_ROUTE_NOT_FOUND"))
                                .build();
                    } else {
                        // Другие типы IllegalArgumentException
                        log.warning("Invalid argument during route deletion (from EJBException): " + message);
                        return Response.status(Response.Status.BAD_REQUEST)
                                .entity(Map.of("error", message, "error_type", "INVALID_ARGUMENT"))
                                .build();
                    }
                } else {
                    log.warning("IllegalArgumentException with null message during route deletion (from EJBException)");
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Некорректные параметры", "error_type", "INVALID_ARGUMENT"))
                            .build();
                }
            } else {
                // Если внутри EJBException не IllegalArgumentException, то обрабатываем как обычную ошибку
                log.severe("EJBException with unexpected cause during route deletion: " + e.getMessage());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of("error", "Не удалось удалить маршрут: " + e.getMessage(), "error_type", "INTERNAL_ERROR"))
                        .build();
            }
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message != null) {
                // Определяем конкретный тип ошибки по сообщению
                if (message.toLowerCase().contains("route not found with id: " + id)) {
                    // Это ошибка именно для удаляемого маршрута
                    log.info("Route " + id + " not found during deletion");
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", "Маршрут не найден", "error_type", "NOT_FOUND"))
                            .build();
                } else if (message.toLowerCase().contains("target route not found") ||
                          message.toLowerCase().contains("coordinates target route not found") ||
                          message.toLowerCase().contains("from location target route not found") ||
                          message.toLowerCase().contains("to location target route not found")) {
                    // Это ошибка в target route для rebinding
                    log.warning("Target route not found during deletion with rebinding: " + message);
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Указанный целевой маршрут не найден: " + message, "error_type", "TARGET_ROUTE_NOT_FOUND"))
                            .build();
                } else {
                    // Другие типы IllegalArgumentException
                    log.warning("Invalid argument during route deletion: " + message);
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", message, "error_type", "INVALID_ARGUMENT"))
                            .build();
                }
            } else {
                log.warning("IllegalArgumentException with null message during route deletion");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Некорректные параметры", "error_type", "INVALID_ARGUMENT"))
                        .build();
            }
        } catch (jakarta.persistence.OptimisticLockException e) {
            // ИСПРАВЛЕНО: Обработка OptimisticLockException при concurrent deletion
            log.info("OptimisticLockException during route " + id + " deletion - likely concurrent deletion, treating as success");
            return Response.noContent().build(); // Маршрут уже удален другой транзакцией
        } catch (Exception e) {
            log.severe("Unexpected error during route deletion: " + e.getMessage());
            e.printStackTrace(); // Для отладки
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Не удалось удалить маршрут", "error_type", "INTERNAL_ERROR"))
                    .build();
        }
    }

    @GET
    @Path("/related/coordinates")
    @Operation(summary = "Получить доступные координаты", description = "Возвращает список доступных координат")
    @ApiResponse(responseCode = "200", description = "Координаты получены успешно")
    public Response getAvailableCoordinates() {
        try {
            List<CoordinatesDto> coordinates = routeService.getAvailableCoordinates();
            return Response.ok(coordinates).build();
        } catch (Exception e) {
            log.severe("Error loading coordinates: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to load coordinates"))
                    .build();
        }
    }
    
    @GET
    @Path("/related/locations")
    @Operation(summary = "Получить доступные локации", description = "Возвращает список доступных локаций")
    @ApiResponse(responseCode = "200", description = "Локации получены успешно")
    public Response getAvailableLocations() {
        try {
            List<LocationDto> locations = routeService.getAvailableLocations();
            return Response.ok(locations).build();
        } catch (Exception e) {
            log.severe("Error loading locations: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to load locations"))
                    .build();
        }
    }

    @GET
    @Path("/{id}/check-dependencies")
    @Operation(summary = "Проверить зависимости маршрута", description = "Проверяет зависимости указанного маршрута")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Зависимости проверены"),
        @ApiResponse(responseCode = "404", description = "Маршрут не найден")
    })
    public Response checkDependencies(@Parameter(description = "ID маршрута") @PathParam("id") Integer id) {
        try {
            Map<String, Object> dependencies = routeService.checkDependencies(id);
            return Response.ok(dependencies).build();
        } catch (jakarta.ejb.EJBException e) {
            // EJB контейнер оборачивает исключения из сервиса в EJBException
            log.info("EJBException during dependency check, checking root cause: " + e.getMessage());
            Throwable rootCause = e.getCause();
            
            if (rootCause instanceof IllegalArgumentException) {
                IllegalArgumentException argEx = (IllegalArgumentException) rootCause;
                if (argEx.getMessage() != null && argEx.getMessage().toLowerCase().contains("not found")) {
                    log.warning("Route not found for dependency check (from EJBException): " + argEx.getMessage());
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", argEx.getMessage(), "error_type", "NOT_FOUND"))
                            .build();
                }
                log.warning("Invalid argument for dependency check (from EJBException): " + argEx.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", argEx.getMessage(), "error_type", "INVALID_ARGUMENT"))
                        .build();
            } else {
                log.severe("EJBException with unexpected cause during dependency check: " + e.getMessage());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of("error", "Не удалось проверить зависимости маршрута", "error_type", "INTERNAL_ERROR"))
                        .build();
            }
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("not found")) {
                log.warning("Route not found for dependency check: " + e.getMessage());
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", e.getMessage(), "error_type", "NOT_FOUND"))
                        .build();
            }
            log.warning("Invalid argument for dependency check: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage(), "error_type", "INVALID_ARGUMENT"))
                    .build();
        } catch (Exception e) {
            log.severe("Error checking dependencies for route " + id + ": " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Не удалось проверить зависимости маршрута", "error_type", "INTERNAL_ERROR"))
                    .build();
        }
    }
}
