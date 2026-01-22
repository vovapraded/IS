package org.example.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import lombok.extern.slf4j.Slf4j;
import org.example.config.cache.CacheLoggingConfigService;
import org.example.config.cache.CacheStatistics;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Producer для EntityManagerFactory с регистрацией Cache MBean
 */
@ApplicationScoped
@Slf4j
public class EntityManagerFactoryProducer {

    @PersistenceUnit(unitName = "RoutesPU")
    private EntityManagerFactory entityManagerFactory;

    @Inject
    private CacheLoggingConfigService cacheLoggingConfigService;

    private ObjectName cacheStatisticsMBeanName;

    @PostConstruct
    public void init() {
        log.info("Инициализация EntityManagerFactory и регистрация Cache MBean");
        registerCacheStatisticsMBean();
    }

    private void registerCacheStatisticsMBean() {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            cacheStatisticsMBeanName = new ObjectName(
                "org.example:type=Cache,name=L2CacheStatistics");

            if (server.isRegistered(cacheStatisticsMBeanName)) {
                server.unregisterMBean(cacheStatisticsMBeanName);
            }

            CacheStatistics cacheStatistics = new CacheStatistics(
                entityManagerFactory, cacheLoggingConfigService);
            server.registerMBean(cacheStatistics, cacheStatisticsMBeanName);

            if (!server.isRegistered(cacheStatisticsMBeanName)) {
                throw new RuntimeException("Cache MBean не зарегистрирован");
            }

            log.info("Cache Statistics JMX MBean зарегистрирован: {}", cacheStatisticsMBeanName);
        } catch (Exception e) {
            log.error("Ошибка регистрации Cache Statistics JMX MBean: {}", e.getMessage(), e);
        }
    }

    @Produces
    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    @PreDestroy
    public void destroy() {
        if (cacheStatisticsMBeanName != null) {
            try {
                MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                if (server.isRegistered(cacheStatisticsMBeanName)) {
                    server.unregisterMBean(cacheStatisticsMBeanName);
                    log.info("Cache Statistics JMX MBean отрегистрирован");
                }
            } catch (Exception e) {
                log.error("Ошибка при отмене регистрации Cache Statistics JMX MBean: {}", e.getMessage());
            }
        }
    }
}