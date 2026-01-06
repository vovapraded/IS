package org.example.exception;

public class RouteConflictException extends RuntimeException {
    public RouteConflictException(String message) {
        super(message);
    }
    
    public RouteConflictException(String message, Throwable cause) {
        super(message, cause);
    }
    
    // Для дублирующихся названий маршрутов
    public static RouteConflictException duplicateRouteName(String name) {
        return new RouteConflictException(String.format(
            "Маршрут с названием '%s' уже существует", name));
    }
    
    // Для дублирующихся координат маршрутов
    public static RouteConflictException duplicateRouteCoordinates(float x, Double y) {
        return new RouteConflictException(String.format(
            "Маршрут с координатами (%.1f, %.1f) уже существует",
            x, y));
    }
    
    // Для маршрутов с нулевым расстоянием
    public static RouteConflictException zeroDistanceRoute(Double fromX, Double fromY, Double toX, Double toY) {
        return new RouteConflictException(String.format(
            "Маршрут не может иметь одинаковые начальную и конечную точки: (%.1f, %.1f)",
            fromX, fromY));
    }
    
    // Для общих constraint violations
    public static RouteConflictException constraintViolation(String details) {
        return new RouteConflictException("Конфликт данных: " + details);
    }
}