package org.example.domain.route.dto;

/**
 * DTO для стандартизированного ответа при операциях с маршрутами
 */
public class RouteResponseDto {
    private RouteDto route;
    private RouteErrorType errorType;
    private String message;
    
    // Конструктор для успешного создания
    public RouteResponseDto(RouteDto route) {
        this.route = route;
        this.errorType = null;
        this.message = null;
    }
    
    // Конструктор для ошибки
    public RouteResponseDto(RouteErrorType errorType, String message) {
        this.route = null;
        this.errorType = errorType;
        this.message = message;
    }
    
    // Конструктор для ошибки с конфликтующим маршрутом
    public RouteResponseDto(RouteDto conflictingRoute, RouteErrorType errorType, String message) {
        this.route = conflictingRoute;
        this.errorType = errorType;
        this.message = message;
    }
    
    // Геттеры и сеттеры
    public RouteDto getRoute() {
        return route;
    }
    
    public void setRoute(RouteDto route) {
        this.route = route;
    }
    
    public RouteErrorType getErrorType() {
        return errorType;
    }
    
    public void setErrorType(RouteErrorType errorType) {
        this.errorType = errorType;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    // Статические фабричные методы для удобства
    public static RouteResponseDto success(RouteDto route) {
        return new RouteResponseDto(route);
    }
    
    public static RouteResponseDto error(RouteErrorType errorType, String message) {
        return new RouteResponseDto(errorType, message);
    }
    
    public static RouteResponseDto conflict(RouteDto conflictingRoute, RouteErrorType errorType, String message) {
        return new RouteResponseDto(conflictingRoute, errorType, message);
    }
}