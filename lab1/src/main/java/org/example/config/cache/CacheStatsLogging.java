package org.example.config.cache;

import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.*;

/**
 * Аннотация для включения логирования статистики L2 кэша
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface CacheStatsLogging {
}