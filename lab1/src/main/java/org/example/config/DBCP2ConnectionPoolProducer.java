package org.example.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import lombok.extern.java.Log;
import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;

/**
 * Producer для Apache Commons DBCP2 Connection Pool
 */
@Singleton
@Startup
@Log
public class DBCP2ConnectionPoolProducer {
    
    private BasicDataSource dbcp2DataSource;
    
    @PostConstruct
    public void init() {
        log.info("Инициализация DBCP2 Connection Pool");
        
        try {
            configureDataSource();
            testConnection();
            registerJMXMonitor();
            log.info("DBCP2 Connection Pool успешно инициализирован");
        } catch (Exception e) {
            log.severe("Ошибка инициализации DBCP2: " + e.getMessage());
            throw new RuntimeException("Не удалось инициализировать DBCP2", e);
        }
    }
    
    private void configureDataSource() {
        dbcp2DataSource = new BasicDataSource();
        
        dbcp2DataSource.setDriverClassName("org.postgresql.Driver");
        dbcp2DataSource.setUrl(getConfigValue("DB_URL"));
        dbcp2DataSource.setUsername(getConfigValue("DB_USERNAME"));
        dbcp2DataSource.setPassword(getConfigValue("DB_PASSWORD"));
        
        dbcp2DataSource.setInitialSize(0);
        dbcp2DataSource.setMinIdle(5);
        dbcp2DataSource.setMaxIdle(15);
        dbcp2DataSource.setMaxTotal(25);
        
        dbcp2DataSource.setValidationQuery("SELECT 1");
        dbcp2DataSource.setTestOnBorrow(true);
        dbcp2DataSource.setTestOnReturn(false);
        dbcp2DataSource.setTestWhileIdle(true);
        
        dbcp2DataSource.setMaxWaitMillis(5000);
        dbcp2DataSource.setTimeBetweenEvictionRunsMillis(30000);
        dbcp2DataSource.setMinEvictableIdleTimeMillis(60000);
        dbcp2DataSource.setMaxConnLifetimeMillis(1800000);
        
        dbcp2DataSource.setValidationQueryTimeout(3);
        dbcp2DataSource.setConnectionInitSqls(java.util.Collections.singletonList("SELECT 1"));
        
        dbcp2DataSource.setRemoveAbandonedOnBorrow(true);
        dbcp2DataSource.setRemoveAbandonedOnMaintenance(true);
        dbcp2DataSource.setRemoveAbandonedTimeout(300);
        dbcp2DataSource.setLogAbandoned(true);
        
        dbcp2DataSource.setDefaultAutoCommit(false);
        dbcp2DataSource.setDefaultTransactionIsolation(2);
        dbcp2DataSource.setPoolPreparedStatements(true);
        dbcp2DataSource.setMaxOpenPreparedStatements(100);
    }
    
    private void testConnection() {
        try (java.sql.Connection testConn = dbcp2DataSource.getConnection()) {
            if (testConn == null || testConn.isClosed()) {
                throw new RuntimeException("Не удалось получить действительное соединение");
            }
            log.info("Тестовое подключение к PostgreSQL успешно");
        } catch (Exception e) {
            log.warning("Ошибка тестового подключения к БД: " + e.getMessage());
            throw new RuntimeException("Не удалось подключиться к базе данных", e);
        }
    }
    
    private void registerJMXMonitor() {
        try {
            javax.management.MBeanServer server = java.lang.management.ManagementFactory.getPlatformMBeanServer();
            javax.management.ObjectName objectName = new javax.management.ObjectName(
                "org.example:type=ConnectionPool,name=DBCP2Monitor");
            
            if (server.isRegistered(objectName)) {
                server.unregisterMBean(objectName);
            }
            
            DBCP2Monitor monitor = new DBCP2Monitor(dbcp2DataSource);
            server.registerMBean(monitor, objectName);
            
            if (!server.isRegistered(objectName)) {
                throw new RuntimeException("MBean не зарегистрирован");
            }
            
            log.info("JMX MBean зарегистрирован: " + objectName);
        } catch (Exception e) {
            log.warning("Ошибка регистрации JMX MBean: " + e.getMessage());
        }
    }
    
    @Produces
    @Named("dbcp2DataSource")
    public DataSource getDBCP2DataSource() {
        return dbcp2DataSource;
    }
    
    public String getDBCP2PoolStats() {
        if (dbcp2DataSource == null) {
            return "DBCP2 DataSource не инициализирован";
        }
        
        return String.format(
            "DBCP2 Stats: Active=%d, Idle=%d, Max=%d, MinIdle=%d, MaxWait=%dms",
            dbcp2DataSource.getNumActive(),
            dbcp2DataSource.getNumIdle(),
            dbcp2DataSource.getMaxTotal(),
            dbcp2DataSource.getMinIdle(),
            dbcp2DataSource.getMaxWaitMillis()
        );
    }
    
    private String getConfigValue(String envVar) {
        String value = System.getenv(envVar);
        if (value == null || value.trim().isEmpty()) {
            // Возвращаем значения по умолчанию для локального развертывания
            switch (envVar) {
                case "DB_URL":
                    return "jdbc:postgresql://localhost:5432/database";
                case "DB_USERNAME":
                    return "admin";
                case "DB_PASSWORD":
                    return "admin";
                default:
                    throw new RuntimeException("Environment variable " + envVar + " is not set or empty");
            }
        }
        return value.trim();
    }
    
    @PreDestroy
    public void destroy() {
        if (dbcp2DataSource != null) {
            try {
                dbcp2DataSource.close();
                log.info("DBCP2 Connection Pool закрыт");
            } catch (Exception e) {
                log.severe("Ошибка при закрытии DBCP2: " + e.getMessage());
            }
        }
    }
}