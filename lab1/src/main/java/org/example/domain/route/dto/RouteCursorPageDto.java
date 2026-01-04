package org.example.domain.route.dto;

import java.util.List;

/**
 * DTO для cursor-based пагинации маршрутов с композитными курсорами
 */
public record RouteCursorPageDto(
    List<RouteDto> routes,
    String nextCursor,      // Encoded композитный cursor следующей страницы
    String prevCursor,      // Encoded композитный cursor предыдущей страницы
    boolean hasNext,
    boolean hasPrev,
    int size,
    long totalCount
) {
    
    /**
     * Создает страницу для первого запроса с композитными курсорами
     */
    public static RouteCursorPageDto first(List<RouteDto> routes, int requestedSize, long totalCount,
                                          String sortBy, String sortDirection) {
        // Восстанавливаем логику определения следующей страницы
        boolean hasNext = routes.size() == requestedSize; // Если получили полную страницу, возможно есть еще
        
        // Создаем простой cursor для следующей страницы (временно без JSON)
        String nextCursor = null;
        if (hasNext && !routes.isEmpty()) {
            RouteDto lastRoute = routes.get(routes.size() - 1);
            nextCursor = "id:" + lastRoute.id(); // Простой cursor для отладки
        }
        
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
     * Создает страницу для следующих запросов с композитными курсорами
     */
    public static RouteCursorPageDto next(List<RouteDto> routes, int requestedSize, long totalCount,
                                         String sortBy, String sortDirection) {
        // Восстанавливаем логику пагинации для next страниц
        boolean hasNext = routes.size() == requestedSize;
        
        String nextCursor = null;
        if (hasNext && !routes.isEmpty()) {
            RouteDto lastRoute = routes.get(routes.size() - 1);
            nextCursor = "id:" + lastRoute.id();
        }
        
        String prevCursor = null;
        if (!routes.isEmpty()) {
            RouteDto firstRoute = routes.get(0);
            prevCursor = "id:" + firstRoute.id();
        }
        
        return new RouteCursorPageDto(
            routes,
            nextCursor,
            prevCursor,
            hasNext,
            true, // Есть предыдущий, так как это не первая страница
            routes.size(),
            totalCount // Передаем реальный count для корректной работы UI
        );
    }
    
    /**
     * Создает страницу для предыдущих запросов с композитными курсорами
     */
    public static RouteCursorPageDto prev(List<RouteDto> routes, int requestedSize, long totalCount,
                                         String sortBy, String sortDirection) {
        // Восстанавливаем логику для prev страниц
        boolean hasPrev = routes.size() == requestedSize;
        
        String nextCursor = null;
        if (!routes.isEmpty()) {
            RouteDto lastRoute = routes.get(routes.size() - 1);
            nextCursor = "id:" + lastRoute.id();
        }
        
        String prevCursor = null;
        if (!routes.isEmpty()) {
            RouteDto firstRoute = routes.get(0);
            prevCursor = "id:" + firstRoute.id();
        }
        
        return new RouteCursorPageDto(
            routes,
            nextCursor,
            prevCursor,
            true, // Есть следующий, так как это не последняя страница
            hasPrev,
            routes.size(),
            totalCount // Передаем реальный count для корректной работы UI
        );
    }
    
    /**
     * Создает композитный cursor на основе данных маршрута и параметров сортировки
     */
    private static CompositeCursor createCursorFromRoute(RouteDto route, String sortBy, String sortDirection) {
        if (route == null) return null;
        
        // Временно упрощаем - возвращаем null для отладки
        return null;
    }
}