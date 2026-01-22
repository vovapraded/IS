package org.example.config.cache;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import lombok.extern.slf4j.Slf4j;

/**
 * CDI Interceptor для логирования статистики L2 кэша
 */
@Interceptor
@CacheStatsLogging
@Priority(1000)
@Slf4j
public class CacheStatsLoggingInterceptor {

    @Inject
    private EntityManagerFactory entityManagerFactory;

    @Inject
    private CacheLoggingConfigService configService;

    @AroundInvoke
    public Object logCacheStatistics(InvocationContext context) throws Exception {
        if (!configService.isCacheLoggingEnabled()) {
            return context.proceed();
        }

        Statistics statisticsBefore = getHibernateStatistics();
        long startTime = System.currentTimeMillis();
        
        // Сохраняем статистику перед выполнением
        CacheStats statsBefore = new CacheStats(statisticsBefore);

        log.info("=== Выполнение метода: {}.{}() ===",
                context.getTarget().getClass().getSimpleName(),
                context.getMethod().getName());
        
        log.info("ПЕРЕД: L2 Cache Hits: {}, Misses: {}, Hit Ratio: {:.2f}%, Puts: {}",
                statsBefore.getCacheHitCount(),
                statsBefore.getCacheMissCount(),
                statsBefore.getCacheHitRatio() * 100,
                statsBefore.getCachePutCount());

        try {
            // Выполняем исходный метод
            Object result = context.proceed();
            
            // Получаем статистику после выполнения
            Statistics statisticsAfter = getHibernateStatistics();
            CacheStats statsAfter = new CacheStats(statisticsAfter);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            log.info("ПОСЛЕ: L2 Cache Hits: {}, Misses: {}, Hit Ratio: {:.2f}%, Puts: {}",
                    statsAfter.getCacheHitCount(),
                    statsAfter.getCacheMissCount(),
                    statsAfter.getCacheHitRatio() * 100,
                    statsAfter.getCachePutCount());
            
            // Рассчитываем дельту
            long deltaHits = statsAfter.getCacheHitCount() - statsBefore.getCacheHitCount();
            long deltaMisses = statsAfter.getCacheMissCount() - statsBefore.getCacheMissCount();
            long deltaPuts = statsAfter.getCachePutCount() - statsBefore.getCachePutCount();
            
            log.info("ДЕЛЬТА: Cache Hits: +{}, Misses: +{}, Puts: +{}, Время выполнения: {} мс",
                    deltaHits, deltaMisses, deltaPuts, executionTime);
            
            log.info("=== Завершение логирования статистики кэша ===");
            
            return result;
            
        } catch (Exception e) {
            log.error("Ошибка при выполнении метода {}.{}(): {}",
                    context.getTarget().getClass().getSimpleName(),
                    context.getMethod().getName(),
                    e.getMessage(), e);
            throw e;
        }
    }

    private Statistics getHibernateStatistics() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        return sessionFactory.getStatistics();
    }

    private static class CacheStats {
        private final long cacheHitCount;
        private final long cacheMissCount;
        private final long cachePutCount;
        private final double cacheHitRatio;

        public CacheStats(Statistics statistics) {
            this.cacheHitCount = statistics.getSecondLevelCacheHitCount();
            this.cacheMissCount = statistics.getSecondLevelCacheMissCount();
            this.cachePutCount = statistics.getSecondLevelCachePutCount();
            
            long totalQueries = cacheHitCount + cacheMissCount;
            this.cacheHitRatio = totalQueries > 0 ? (double) cacheHitCount / totalQueries : 0.0;
        }

        public long getCacheHitCount() { return cacheHitCount; }
        public long getCacheMissCount() { return cacheMissCount; }
        public long getCachePutCount() { return cachePutCount; }
        public double getCacheHitRatio() { return cacheHitRatio; }
    }
}