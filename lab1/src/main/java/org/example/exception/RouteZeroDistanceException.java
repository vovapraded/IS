package org.example.exception;

/**
 * Исключение для случая, когда маршрут создается с нулевым расстоянием между одинаковыми точками
 */
public class RouteZeroDistanceException extends ValidationException {
    
    public RouteZeroDistanceException(Double fromX, Double fromY, Double toX, Double toY) {
        super("Нельзя создать маршрут с одинаковыми начальной и конечной точками: (" + fromX + ", " + fromY + ") -> (" + toX + ", " + toY + ")");
    }
}