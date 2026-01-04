package org.example.exception;

/**
 * Исключение для случая, когда маршрут с таким именем уже существует
 */
public class RouteNameAlreadyExistsException extends ValidationException {
    
    public RouteNameAlreadyExistsException(String routeName) {
        super("Маршрут с именем '" + routeName + "' уже существует в системе");
    }
    
    public RouteNameAlreadyExistsException(String routeName, Integer existingRouteId) {
        super("Маршрут с именем '" + routeName + "' уже существует (ID: " + existingRouteId + ")");
    }
}