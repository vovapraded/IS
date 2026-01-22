package org.example.config.cache;

import java.util.Map;

/**
 * MBean интерфейс для мониторинга L2 JPA Cache
 */
public interface CacheStatisticsMBean {
    
    // Основная статистика кэша
    long getSecondLevelCacheHitCount();
    long getSecondLevelCacheMissCount();
    long getSecondLevelCachePutCount();
    double getSecondLevelCacheHitRatio();
    String getSecondLevelCacheHitRatioPercent();
    
    // Статистика запросов  
    long getQueryExecutionCount();
    long getQueryCacheHitCount();
    long getQueryCacheMissCount();
    long getQueryCachePutCount();
    
    // Статистика сессий
    long getSessionOpenCount();
    long getSessionCloseCount();
    long getTransactionCount();
    long getSuccessfulTransactionCount();
    
    // Управление статистикой
    boolean isStatisticsEnabled();
    void enableStatistics();
    void disableStatistics();
    void clearStatistics();
    
    // Управление логированием
    boolean isCacheLoggingEnabled();
    void enableCacheLogging();
    void disableCacheLogging();
    
    // Информация о регионах кэша
    String[] getCacheRegionNames();
    Map<String, Object> getCacheRegionStatistics(String regionName);
    String getAllRegionsStatistics();
    
    // Служебные методы
    String getCacheStatisticsSummary();
    void logCacheStatistics();
    String getHibernateVersion();
    String getEhcacheVersion();
    long getStatisticsTimestamp();
}