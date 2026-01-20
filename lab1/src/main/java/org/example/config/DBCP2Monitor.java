package org.example.config;

import lombok.extern.java.Log;
import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Реализация MBean для мониторинга DBCP2 Connection Pool
 */
@Log
public class DBCP2Monitor implements DBCP2MonitorMBean {
    
    private final BasicDataSource dataSource;
    
    public DBCP2Monitor(BasicDataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public int getActiveConnections() {
        return dataSource.getNumActive();
    }
    
    @Override
    public int getIdleConnections() {
        return dataSource.getNumIdle();
    }
    
    @Override
    public int getMaxTotalConnections() {
        return dataSource.getMaxTotal();
    }
    
    @Override
    public int getMinIdleConnections() {
        return dataSource.getMinIdle();
    }
    
    @Override
    public int getMaxIdleConnections() {
        return dataSource.getMaxIdle();
    }
    
    @Override
    public String getDatabaseUrl() {
        return dataSource.getUrl();
    }
    
    @Override
    public String getDriverClassName() {
        return dataSource.getDriverClassName();
    }
    
    @Override
    public boolean isTestOnBorrow() {
        return dataSource.getTestOnBorrow();
    }
    
    @Override
    public String getValidationQuery() {
        return dataSource.getValidationQuery();
    }
    
    @Override
    public long getMaxWaitMillis() {
        return dataSource.getMaxWaitMillis();
    }
    
    @Override
    public String getPoolStatistics() {
        return String.format(
            "DBCP2 Pool Stats: Active=%d, Idle=%d, MaxTotal=%d, MinIdle=%d, MaxIdle=%d, MaxWait=%dms",
            getActiveConnections(),
            getIdleConnections(),
            getMaxTotalConnections(),
            getMinIdleConnections(),
            getMaxIdleConnections(),
            getMaxWaitMillis()
        );
    }
    
    @Override
    public void logPoolStatus() {
        log.info("DBCP2 Pool Status");
        log.info("Active Connections: " + getActiveConnections());
        log.info("Idle Connections: " + getIdleConnections());
        log.info("Max Total: " + getMaxTotalConnections());
        log.info("Database URL: " + getDatabaseUrl());
        log.info("Driver: " + getDriverClassName());
        log.info("Test On Borrow: " + isTestOnBorrow());
        log.info("Validation Query: " + getValidationQuery());
    }
    
    @Override
    public boolean isHealthy() {
        try {
            try (Connection conn = dataSource.getConnection()) {
                return conn != null && !conn.isClosed();
            }
        } catch (SQLException e) {
            log.warning("Health check failed: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getDBCP2Version() {
        return "Apache Commons DBCP2 v2.11.0";
    }
    
    @Override
    public String getConnectionPoolType() {
        return "Apache Commons DBCP2 BasicDataSource";
    }
}