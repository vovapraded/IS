package org.example.exception;

/**
 * Исключение для случая, когда маршрут создается с нулевым расстоянием между одинаковыми точками
 */
public class RouteZeroDistanceException extends ValidationException {
    
    private final Double fromX;
    private final Double fromY;
    private final Double toX;
    private final Double toY;
    
    public RouteZeroDistanceException(Double fromX, Double fromY, Double toX, Double toY) {
        super("Нельзя создать маршрут с одинаковыми начальной и конечной точками: (" + fromX + ", " + fromY + ") -> (" + toX + ", " + toY + ")");
        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
    }
    
    public Double getFromX() {
        return fromX;
    }
    
    public Double getFromY() {
        return fromY;
    }
    
    public Double getToX() {
        return toX;
    }
    
    public Double getToY() {
        return toY;
    }
}