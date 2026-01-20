package org.example.config;

/**
 * MBean интерфейс для мониторинга DBCP2 Connection Pool
 */
public interface DBCP2MonitorMBean {
    
    int getActiveConnections();
    int getIdleConnections();
    int getMaxTotalConnections();
    int getMinIdleConnections();
    int getMaxIdleConnections();
    
    String getDatabaseUrl();
    String getDriverClassName();
    boolean isTestOnBorrow();
    String getValidationQuery();
    long getMaxWaitMillis();
    
    String getPoolStatistics();
    void logPoolStatus();
    boolean isHealthy();
    
    String getDBCP2Version();
    String getConnectionPoolType();
}