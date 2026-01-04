package org.example.exception;

/**
 * Исключение для случая, когда маршрут с такими координатами уже существует
 */
public class RouteCoordinatesAlreadyExistException extends ValidationException {
    
    public RouteCoordinatesAlreadyExistException(float x, Double y) {
        super("Маршрут с координатами (" + x + ", " + y + ") уже существует в системе");
    }
    
    public RouteCoordinatesAlreadyExistException(float x, Double y, Integer existingRouteId) {
        super("Маршрут с координатами (" + x + ", " + y + ") уже существует (ID: " + existingRouteId + ")");
    }
}