package org.example.domain.route.dto;

/**
 * Enum для типов ошибок при работе с маршрутами
 */
public enum RouteErrorType {
    /**
     * Маршрут с таким именем уже существует
     */
    DUPLICATE_NAME,
    
    /**
     * Маршрут с нулевым расстоянием (одинаковые точки начала и конца)
     */
    ZERO_DISTANCE_ROUTE,
    
    /**
     * Ошибка валидации данных
     */
    VALIDATION_ERROR,
    
    /**
     * Некорректные аргументы
     */
    INVALID_ARGUMENT,
    
    /**
     * Ошибка состояния сервиса
     */
    STATE_ERROR,
    
    /**
     * Runtime ошибка
     */
    RUNTIME_ERROR,
    
    /**
     * Внутренняя ошибка сервера
     */
    INTERNAL_ERROR,
    
    /**
     * Объект не найден
     */
    NOT_FOUND
}