package org.example.exception;

/**
 * Базовое исключение для ошибок валидации данных
 */
public abstract class ValidationException extends RuntimeException {
    
    protected ValidationException(String message) {
        super(message);
    }
    
    protected ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}