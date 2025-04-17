package com.privsense.db.service;

import com.privsense.db.config.DatabaseConnectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * Service responsible for dynamically loading JDBC drivers at runtime.
 * This allows supporting multiple database types without bundling all drivers.
 */
@Service
public class JdbcDriverLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(JdbcDriverLoader.class);
    
    private final DatabaseConnectionConfig connectionConfig;
    private final Set<String> loadedDrivers = new HashSet<>();
    
    @Autowired
    public JdbcDriverLoader(DatabaseConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }
    
    /**
     * Loads a JDBC driver class if it's not already loaded.
     * 
     * @param driverClassName The fully qualified class name of the JDBC driver
     * @return true if the driver was loaded successfully or was already loaded, false otherwise
     */
    public synchronized boolean loadDriver(String driverClassName) {
        if (driverClassName == null || driverClassName.isEmpty()) {
            logger.error("Driver class name cannot be null or empty");
            return false;
        }
        
        // Check if the driver is already loaded
        if (isDriverLoaded(driverClassName)) {
            logger.debug("Driver '{}' is already loaded", driverClassName);
            return true;
        }
        
        // Try loading the driver directly (it might be on the classpath)
        try {
            Class.forName(driverClassName);
            loadedDrivers.add(driverClassName);
            logger.info("Successfully loaded JDBC driver '{}' from classpath", driverClassName);
            return true;
        } catch (ClassNotFoundException e) {
            logger.debug("Driver '{}' not found on classpath, will attempt to load from external directory", driverClassName);
        }
        
        // If not found on classpath, try loading from external directory
        return loadDriverFromExternalDirectory(driverClassName);
    }
    
    /**
     * Checks if a specific driver class is already loaded.
     * 
     * @param driverClassName The class name to check
     * @return true if the driver is already registered
     */
    private boolean isDriverLoaded(String driverClassName) {
        if (loadedDrivers.contains(driverClassName)) {
            return true;
        }
        
        // Also check if registered with DriverManager
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getName().equals(driverClassName)) {
                loadedDrivers.add(driverClassName);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Attempts to load a driver from JAR files in the configured external directory.
     * 
     * @param driverClassName The driver class to load
     * @return true if the driver was loaded successfully
     */
    private boolean loadDriverFromExternalDirectory(String driverClassName) {
        String driverDir = connectionConfig.getJdbc().getDriverDir();
        Path dirPath = Paths.get(driverDir);
        File dir = dirPath.toFile();
        
        if (!dir.exists() || !dir.isDirectory()) {
            logger.error("Driver directory '{}' does not exist or is not a directory", driverDir);
            return false;
        }
        
        File[] jarFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".jar"));
        
        if (jarFiles == null || jarFiles.length == 0) {
            logger.error("No JAR files found in driver directory '{}'", driverDir);
            return false;
        }
        
        URLClassLoader classLoader = null;
        
        try {
            URL[] urls = new URL[jarFiles.length];
            
            for (int i = 0; i < jarFiles.length; i++) {
                urls[i] = jarFiles[i].toURI().toURL();
                logger.debug("Added JAR to classpath: {}", jarFiles[i].getName());
            }
            
            classLoader = new URLClassLoader(urls, JdbcDriverLoader.class.getClassLoader());
            
            // Try to load the driver class from the JAR files
            Class<?> driverClass = Class.forName(driverClassName, true, classLoader);
            
            // Ensure the class actually implements java.sql.Driver
            if (!Driver.class.isAssignableFrom(driverClass)) {
                throw new ClassCastException("Class " + driverClassName + " does not implement java.sql.Driver");
            }
            
            // No need to register with DriverManager explicitly since JDBC 4.0
            // The driver registers itself when the class is loaded
            
            loadedDrivers.add(driverClassName);
            logger.info("Successfully loaded JDBC driver '{}' from external directory", driverClassName);
            
            return true;
        } catch (MalformedURLException e) {
            logger.error("Invalid URL for JAR file", e);
        } catch (ClassNotFoundException e) {
            logger.error("Driver class '{}' not found in the provided JAR files", driverClassName, e);
        } catch (Exception e) {
            logger.error("Failed to load driver class '{}'", driverClassName, e);
        }
        
        return false;
    }
    
    /**
     * Returns the current driver directory path
     */
    public String getDriverDirectory() {
        return connectionConfig.getJdbc().getDriverDir();
    }
}