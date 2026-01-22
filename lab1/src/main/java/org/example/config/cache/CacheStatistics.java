package org.example.config.cache;

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import jakarta.persistence.EntityManagerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Реализация MBean для мониторинга L2 JPA Cache
 */
@Slf4j
public class CacheStatistics implements CacheStatisticsMBean {

    private final EntityManagerFactory entityManagerFactory;
    private final CacheLoggingConfigService cacheLoggingConfigService;

    public CacheStatistics(EntityManagerFactory entityManagerFactory, 
                          CacheLoggingConfigService cacheLoggingConfigService) {
        this.entityManagerFactory = entityManagerFactory;
        this.cacheLoggingConfigService = cacheLoggingConfigService;
    }

    private Statistics getHibernateStatistics() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        return sessionFactory.getStatistics();
    }

    @Override
    public long getSecondLevelCacheHitCount() {
        return getHibernateStatistics().getSecondLevelCacheHitCount();
    }

    @Override
    public long getSecondLevelCacheMissCount() {
        return getHibernateStatistics().getSecondLevelCacheMissCount();
    }

    @Override
    public long getSecondLevelCachePutCount() {
        return getHibernateStatistics().getSecondLevelCachePutCount();
    }

    @Override
    public double getSecondLevelCacheHitRatio() {
        Statistics stats = getHibernateStatistics();
        long hits = stats.getSecondLevelCacheHitCount();
        long misses = stats.getSecondLevelCacheMissCount();
        long total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }

    @Override
    public String getSecondLevelCacheHitRatioPercent() {
        return String.format("%.2f%%", getSecondLevelCacheHitRatio() * 100);
    }

    @Override
    public long getQueryExecutionCount() {
        return getHibernateStatistics().getQueryExecutionCount();
    }

    @Override
    public long getQueryCacheHitCount() {
        return getHibernateStatistics().getQueryCacheHitCount();
    }

    @Override
    public long getQueryCacheMissCount() {
        return getHibernateStatistics().getQueryCacheMissCount();
    }

    @Override
    public long getQueryCachePutCount() {
        return getHibernateStatistics().getQueryCachePutCount();
    }

    @Override
    public long getSessionOpenCount() {
        return getHibernateStatistics().getSessionOpenCount();
    }

    @Override
    public long getSessionCloseCount() {
        return getHibernateStatistics().getSessionCloseCount();
    }

    @Override
    public long getTransactionCount() {
        return getHibernateStatistics().getTransactionCount();
    }

    @Override
    public long getSuccessfulTransactionCount() {
        return getHibernateStatistics().getSuccessfulTransactionCount();
    }

    @Override
    public boolean isStatisticsEnabled() {
        return getHibernateStatistics().isStatisticsEnabled();
    }

    @Override
    public void enableStatistics() {
        getHibernateStatistics().setStatisticsEnabled(true);
        log.info("Hibernate statistics enabled via JMX");
    }

    @Override
    public void disableStatistics() {
        getHibernateStatistics().setStatisticsEnabled(false);
        log.info("Hibernate statistics disabled via JMX");
    }

    @Override
    public void clearStatistics() {
        getHibernateStatistics().clear();
        log.info("Hibernate statistics cleared via JMX");
    }

    @Override
    public boolean isCacheLoggingEnabled() {
        return cacheLoggingConfigService.isCacheLoggingEnabled();
    }

    @Override
    public void enableCacheLogging() {
        cacheLoggingConfigService.enableCacheLogging();
        log.info("Cache logging enabled via JMX");
    }

    @Override
    public void disableCacheLogging() {
        cacheLoggingConfigService.disableCacheLogging();
        log.info("Cache logging disabled via JMX");
    }

    @Override
    public String[] getCacheRegionNames() {
        return getHibernateStatistics().getSecondLevelCacheRegionNames();
    }

    @Override
    public Map<String, Object> getCacheRegionStatistics(String regionName) {
        Map<String, Object> regionStats = new HashMap<>();
        
        try {
            regionStats.put("name", regionName);
            // Hibernate Statistics API не предоставляет детальную статистику по отдельным регионам
            // Возвращаем общую информацию и указание на использование JMX для детальной статистики
            regionStats.put("note", "Детальная статистика региона доступна только через JMX");
            regionStats.put("availableViaJMX", true);
            regionStats.put("generalL2CacheHits", getSecondLevelCacheHitCount());
            regionStats.put("generalL2CacheMisses", getSecondLevelCacheMissCount());
        } catch (Exception e) {
            regionStats.put("error", "Region information unavailable: " + e.getMessage());
        }
        
        return regionStats;
    }

    @Override
    public String getAllRegionsStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== L2 Cache Regions Statistics ===\n");
        
        String[] regions = getCacheRegionNames();
        for (String region : regions) {
            sb.append("Region: ").append(region).append("\n");
            Map<String, Object> stats = getCacheRegionStatistics(region);
            for (Map.Entry<String, Object> entry : stats.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }

    @Override
    public String getCacheStatisticsSummary() {
        Statistics stats = getHibernateStatistics();
        
        return String.format(
            "L2 Cache Summary:\n" +
            "- Statistics Enabled: %s\n" +
            "- Cache Logging Enabled: %s\n" +
            "- Hit Count: %d\n" +
            "- Miss Count: %d\n" +
            "- Put Count: %d\n" +
            "- Hit Ratio: %s\n" +
            "- Query Executions: %d\n" +
            "- Query Cache Hits: %d\n" +
            "- Sessions Opened: %d\n" +
            "- Transactions: %d\n" +
            "- Cache Regions: %d\n" +
            "- Timestamp: %d",
            isStatisticsEnabled(),
            isCacheLoggingEnabled(),
            getSecondLevelCacheHitCount(),
            getSecondLevelCacheMissCount(),
            getSecondLevelCachePutCount(),
            getSecondLevelCacheHitRatioPercent(),
            getQueryExecutionCount(),
            getQueryCacheHitCount(),
            getSessionOpenCount(),
            getTransactionCount(),
            getCacheRegionNames().length,
            getStatisticsTimestamp()
        );
    }

    @Override
    public void logCacheStatistics() {
        log.info("=== L2 Cache Statistics (via JMX) ===");
        log.info("Hit Count: {}", getSecondLevelCacheHitCount());
        log.info("Miss Count: {}", getSecondLevelCacheMissCount());
        log.info("Put Count: {}", getSecondLevelCachePutCount());
        log.info("Hit Ratio: {}", getSecondLevelCacheHitRatioPercent());
        log.info("Query Executions: {}", getQueryExecutionCount());
        log.info("Active Regions: {}", String.join(", ", getCacheRegionNames()));
        log.info("Statistics Enabled: {}", isStatisticsEnabled());
        log.info("Cache Logging Enabled: {}", isCacheLoggingEnabled());
    }

    @Override
    public String getHibernateVersion() {
        return org.hibernate.Version.getVersionString();
    }

    @Override
    public String getEhcacheVersion() {
        try {
            return "Ehcache 3.x (integrated with Hibernate)";
        } catch (Exception e) {
            return "Version information unavailable";
        }
    }

    @Override
    public long getStatisticsTimestamp() {
        return System.currentTimeMillis();
    }
}