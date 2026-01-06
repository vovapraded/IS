package org.example.exception;

import org.example.domain.route.dto.RouteDto;

/**
 * Исключение для случая, когда маршрут с таким именем уже существует
 */
public class RouteNameAlreadyExistsException extends ValidationException {
    
    private final String routeName;
    private final RouteDto conflictingRoute;
    
    public RouteNameAlreadyExistsException(String routeName) {
        super("Маршрут с именем '" + routeName + "' уже существует в системе");
        this.routeName = routeName;
        this.conflictingRoute = null;
    }
    
    public RouteNameAlreadyExistsException(String routeName, Integer existingRouteId) {
        super("Маршрут с именем '" + routeName + "' уже существует (ID: " + existingRouteId + ")");
        this.routeName = routeName;
        this.conflictingRoute = null;
    }
    
    public RouteNameAlreadyExistsException(String routeName, RouteDto conflictingRoute) {
        super("Маршрут с именем '" + routeName + "' уже существует в системе");
        this.routeName = routeName;
        this.conflictingRoute = conflictingRoute;
    }
    
    public String getRouteName() {
        return routeName;
    }
    
    public RouteDto getConflictingRoute() {
        return conflictingRoute;
    }
}