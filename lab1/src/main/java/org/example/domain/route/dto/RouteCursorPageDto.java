package org.example.domain.route.dto;

import java.util.List;

/**
 * DTO для cursor-based пагинации маршрутов
 */
public record RouteCursorPageDto(
    List<RouteDto> routes,
    String nextCursor,
    String prevCursor,
    boolean hasNext,
    boolean hasPrev,
    int size,
    long totalCount
) {
    
    /**
     * Создает страницу для первого запроса
     */
    public static RouteCursorPageDto first(List<RouteDto> routes, int requestedSize, long totalCount) {
        String nextCursor = routes.isEmpty() ? null : routes.get(routes.size() - 1).id().toString();
        boolean hasNext = routes.size() == requestedSize; // Если получили полную страницу, возможно есть еще
        
        return new RouteCursorPageDto(
            routes,
            nextCursor,
            null, // Нет предыдущего для первой страницы
            hasNext,
            false, // Нет предыдущего для первой страницы
            routes.size(),
            totalCount
        );
    }
    
    /**
     * Создает страницу для следующих запросов
     */
    public static RouteCursorPageDto next(List<RouteDto> routes, String prevCursor, int requestedSize) {
        String nextCursor = routes.isEmpty() ? null : routes.get(routes.size() - 1).id().toString();
        boolean hasNext = routes.size() == requestedSize;
        
        return new RouteCursorPageDto(
            routes,
            nextCursor,
            prevCursor,
            hasNext,
            true, // Есть предыдущий, так как это не первая страница
            routes.size(),
            -1 // Не вычисляем общий count для cursor-based
        );
    }
    
    /**
     * Создает страницу для предыдущих запросов
     */
    public static RouteCursorPageDto prev(List<RouteDto> routes, String nextCursor, int requestedSize) {
        String prevCursor = routes.isEmpty() ? null : routes.get(0).id().toString();
        boolean hasPrev = routes.size() == requestedSize;
        
        return new RouteCursorPageDto(
            routes,
            nextCursor,
            prevCursor,
            true, // Есть следующий, так как это не последняя страница
            hasPrev,
            routes.size(),
            -1 // Не вычисляем общий count для cursor-based
        );
    }
}