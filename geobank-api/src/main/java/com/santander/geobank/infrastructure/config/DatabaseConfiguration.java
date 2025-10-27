package com.santander.geobank.infrastructure.config;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Production-ready database configuration.
 * Applies Martin Fowler's enterprise patterns.
 *
 * Design Patterns Applied:
 * - Repository Pattern (via JPA)
 * - Unit of Work (via JPA/Hibernate)
 * - Connection Pool Pattern (via HikariCP)
 * - Transaction Script (via @Transactional)
 */
@Configuration
@EnableTransactionManagement
public class DatabaseConfiguration {

    /**
     * Primary datasource with production-ready connection pooling.
     * Applies Enterprise Integration Patterns.
     */
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "geobank.datasource")
    public DataSource primaryDataSource() {
        return createOptimizedDataSource();
    }

    /**
     * Entity Manager Factory with optimized settings.
     * Implements Unit of Work pattern.
     */
    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.santander.geobank.infrastructure.persistence.entities");

        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setGenerateDdl(true);
        adapter.setShowSql(false);
        factory.setJpaVendorAdapter(adapter);

        factory.setJpaProperties(createHibernateProperties());

        return factory;
    }

    /**
     * Transaction Manager with proper isolation levels.
     * Implements Transaction Script pattern.
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(
            LocalContainerEntityManagerFactoryBean entityManagerFactory) {

        JpaTransactionManager manager = new JpaTransactionManager();
        manager.setEntityManagerFactory(entityManagerFactory.getObject());
        manager.setDefaultTimeout(30);

        return manager;
    }

    /**
     * Health indicator for database connectivity.
     */
    @Bean
    public DatabaseHealthService databaseHealthService(DataSource dataSource) {
        return new DatabaseHealthService(dataSource);
    }

    /**
     * Create optimized HikariCP datasource.
     * Private method following Fowler's refactoring principles.
     */
    private HikariDataSource createOptimizedDataSource() {
        HikariConfig config = new HikariConfig();

        // Connection pool optimization
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        // Banking-grade connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);

        // Demo configuration (externalize in production)
        config.setJdbcUrl("jdbc:h2:mem:geobank;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        config.setDriverClassName("org.h2.Driver");
        config.setUsername("sa");
        config.setPassword("password");

        // Performance optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        return new HikariDataSource(config);
    }

    /**
     * Create Hibernate properties with banking optimizations.
     * Extract Method refactoring applied.
     */
    private java.util.Properties createHibernateProperties() {
        java.util.Properties properties = new java.util.Properties();

        // Basic configuration
        properties.put("hibernate.hbm2ddl.auto", "create-drop");
        properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.put("hibernate.format_sql", "true");

        // Performance optimizations
        properties.put("hibernate.jdbc.batch_size", "25");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");

        // Connection management
        properties.put("hibernate.connection.provider_disables_autocommit", "true");

        // Monitoring
        properties.put("hibernate.generate_statistics", "true");

        return properties;
    }

    /**
     * Database health service following Service Layer pattern.
     */
    public static class DatabaseHealthService {
        private final DataSource dataSource;

        public DatabaseHealthService(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        public boolean isHealthy() {
            try (var connection = dataSource.getConnection()) {
                return connection.isValid(5);
            } catch (Exception e) {
                return false;
            }
        }

        public ConnectionPoolStatistics getConnectionPoolStats() {
            if (dataSource instanceof HikariDataSource hikari) {
                var mxBean = hikari.getHikariPoolMXBean();
                return new ConnectionPoolStatistics(
                        mxBean.getActiveConnections(),
                        mxBean.getIdleConnections(),
                        mxBean.getTotalConnections());
            }
            return new ConnectionPoolStatistics(0, 0, 0);
        }
    }

    /**
     * Value Object for connection pool statistics.
     * Follows Fowler's Value Object pattern.
     */
    public record ConnectionPoolStatistics(
            int activeConnections,
            int idleConnections,
            int totalConnections) {
        public double getUtilizationPercentage() {
            return totalConnections > 0 ? (double) activeConnections / totalConnections * 100 : 0.0;
        }
    }
}

