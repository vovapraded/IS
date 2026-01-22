package org.example.config.cache;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Сервис для управления настройками логирования L2 кэша
 */
@ApplicationScoped
@Slf4j
public class CacheLoggingConfigService {
    
    private final AtomicBoolean cacheLoggingEnabled = new AtomicBoolean(false);

    /**
     * Проверяет, включено ли логирование статистики L2 кэша
     * @return true если логирование включено
     */
    public boolean isCacheLoggingEnabled() {
        return cacheLoggingEnabled.get();
    }

    /**
     * Включает логирование статистики L2 кэша
     */
    public void enableCacheLogging() {
        boolean wasEnabled = cacheLoggingEnabled.getAndSet(true);
        if (!wasEnabled) {
            log.info("Логирование статистики L2 кэша ВКЛЮЧЕНО");
        }
    }

    /**
     * Отключает логирование статистики L2 кэша
     */
    public void disableCacheLogging() {
        boolean wasEnabled = cacheLoggingEnabled.getAndSet(false);
        if (wasEnabled) {
            log.info("Логирование статистики L2 кэша ОТКЛЮЧЕНО");
        }
    }

    /**
     * Переключает состояние логирования статистики L2 кэша
     * @return новое состояние (true - включено, false - отключено)
     */
    public boolean toggleCacheLogging() {
        boolean newState = !cacheLoggingEnabled.get();
        cacheLoggingEnabled.set(newState);
        
        if (newState) {
            log.info("Логирование статистики L2 кэша ВКЛЮЧЕНО");
        } else {
            log.info("Логирование статистики L2 кэша ОТКЛЮЧЕНО");
        }
        
        return newState;
    }

    /**
     * Получает текущий статус логирования в текстовом виде
     * @return статус логирования
     */
    public String getLoggingStatus() {
        return cacheLoggingEnabled.get() ? "ВКЛЮЧЕНО" : "ОТКЛЮЧЕНО";
    }
}