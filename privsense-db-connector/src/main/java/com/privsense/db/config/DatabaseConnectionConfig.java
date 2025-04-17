package com.privsense.db.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for database connections.
 * Maps properties with the prefix "privsense.db" from application.yml/properties.
 */
@Configuration
@ConfigurationProperties(prefix = "privsense.db")
public class DatabaseConnectionConfig {
    
    private final Pool pool = new Pool();
    private final Jdbc jdbc = new Jdbc();
    
    /**
     * Connection pool specific configuration
     */
    public static class Pool {
        private long connectionTimeout = 30000; // 30 seconds
        private long idleTimeout = 600000; // 10 minutes
        private long maxLifetime = 1800000; // 30 minutes
        private int minimumIdle = 5;
        private int maximumPoolSize = 10;
        
        public long getConnectionTimeout() {
            return connectionTimeout;
        }
        
        public void setConnectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }
        
        public long getIdleTimeout() {
            return idleTimeout;
        }
        
        public void setIdleTimeout(long idleTimeout) {
            this.idleTimeout = idleTimeout;
        }
        
        public long getMaxLifetime() {
            return maxLifetime;
        }
        
        public void setMaxLifetime(long maxLifetime) {
            this.maxLifetime = maxLifetime;
        }
        
        public int getMinimumIdle() {
            return minimumIdle;
        }
        
        public void setMinimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
        }
        
        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }
        
        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }
    }
    
    /**
     * JDBC driver specific configuration
     */
    public static class Jdbc {
        private String driverDir = "./drivers";
        
        public String getDriverDir() {
            return driverDir;
        }
        
        public void setDriverDir(String driverDir) {
            this.driverDir = driverDir;
        }
    }
    
    public Pool getPool() {
        return pool;
    }
    
    public Jdbc getJdbc() {
        return jdbc;
    }
}