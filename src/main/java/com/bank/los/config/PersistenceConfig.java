package com.bank.los.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {
                "com.bank.los.administration.master.repository",
                "com.bank.los.administration.audit.repository",
                "com.bank.los.bank.master.repository",
                "com.bank.los.bank.lead.repository",
                "com.bank.los.bank.product.repository",
                "com.bank.los.bank.loan.repository",
                "com.bank.los.bank.audit.repository"
        },
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
)
public class PersistenceConfig {

    @Value("${master.datasource.url:jdbc:postgresql://localhost:5432/los_master_db}")
    private String masterUrl;

    @Value("${master.datasource.username:postgres}")
    private String masterUsername;

    @Value("${master.datasource.password:postgres}")
    private String masterPassword;

    @Value("${master.datasource.driver-class-name:org.postgresql.Driver}")
    private String masterDriverClassName;

    @Bean(name = "masterDataSource")
    public DataSource masterDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName(masterDriverClassName);

        ConnectionDetails details = parseConnectionDetails(masterUrl, masterUsername, masterPassword);
        ds.setJdbcUrl(details.jdbcUrl());
        ds.setUsername(details.username());
        ds.setPassword(details.password());
        ds.setPoolName("MasterHikariPool");
        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(2);
        ds.setIdleTimeout(30000);
        ds.setConnectionTimeout(30000);

        // Ensure master schema exists if script is present
        try (java.sql.Connection conn = ds.getConnection()) {
            org.springframework.core.io.ClassPathResource masterSchema =
                    new org.springframework.core.io.ClassPathResource("db/master-schema.sql");
            if (masterSchema.exists()) {
                org.springframework.jdbc.datasource.init.ResourceDatabasePopulator populator =
                        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator();
                populator.addScript(masterSchema);
                populator.setContinueOnError(true);
                populator.setIgnoreFailedDrops(true);
                populator.populate(conn);
            }
            migrateLegacyInstitutionColumns(conn);
        } catch (Exception e) {
            // Ignore if already created or offline during build
        }

        return ds;
    }

    @Bean(name = "routingDataSource")
    @Primary
    public DataSource routingDataSource(BankDataSourceProvider bankDataSourceProvider,
                                        @Qualifier("masterDataSource") DataSource masterDataSource) {
        MultiBankRoutingDataSource routingDataSource = new MultiBankRoutingDataSource(bankDataSourceProvider, masterDataSource);
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(BankContext.MASTER_BANK_ID, masterDataSource);

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        routingDataSource.afterPropertiesSet();
        return routingDataSource;
    }

    @Bean(name = "entityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("routingDataSource") DataSource routingDataSource,
            JpaProperties jpaProperties,
            org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties hibernateProperties) {
        Map<String, Object> properties = hibernateProperties.determineHibernateProperties(
                jpaProperties.getProperties(), new org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings());

        // Explicitly disable eager JDBC metadata access during startup
        properties.put("hibernate.temp.use_jdbc_metadata_defaults", "false");

        return builder
                .dataSource(routingDataSource)
                .packages(
                        "com.bank.los.administration.master.entity",
                        "com.bank.los.administration.audit.entity",
                        "com.bank.los.bank.master.entity",
                        "com.bank.los.bank.lead.entity",
                        "com.bank.los.bank.product.entity",
                        "com.bank.los.bank.loan.entity",
                        "com.bank.los.bank.audit.entity"
                )
                .persistenceUnit("losPersistenceUnit")
                .properties(properties)
                .build();
    }

    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    public static ConnectionDetails parseConnectionDetails(String rawUrl, String defaultUser, String defaultPass) {
        if (rawUrl == null || rawUrl.isEmpty()) {
            return new ConnectionDetails(rawUrl, defaultUser, defaultPass);
        }
        String url = rawUrl.trim();
        if (url.startsWith("postgresql://") || url.startsWith("postgres://")) {
            try {
                String uriString = url.replaceFirst("^(postgresql|postgres)://", "http://");
                java.net.URI uri = new java.net.URI(uriString);
                String userInfo = uri.getUserInfo();
                String username = defaultUser;
                String password = defaultPass;
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    username = parts[0];
                    password = parts[1];
                } else if (userInfo != null && !userInfo.isEmpty()) {
                    username = userInfo;
                }
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();
                String dbName = (path != null && path.length() > 1) ? path.substring(1) : "los_master_db";
                String host = uri.getHost();
                if (host != null && host.startsWith("dpg-") && !host.contains(".")) {
                    host = host + ".singapore-postgres.render.com";
                }
                String query = uri.getQuery();
                if (host != null && (host.contains("render.com") || host.startsWith("dpg-")) && (query == null || !query.contains("sslmode"))) {
                    query = (query == null || query.isEmpty()) ? "sslmode=require" : query + "&sslmode=require";
                }
                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName + (query != null && !query.isEmpty() ? "?" + query : "");
                return new ConnectionDetails(jdbcUrl, username, password);
            } catch (Exception e) {
                if (!url.startsWith("jdbc:")) {
                    return new ConnectionDetails("jdbc:" + url, defaultUser, defaultPass);
                }
            }
        }
        if (url.startsWith("jdbc:postgresql://dpg-") && !url.contains(".render.com")) {
            url = url.replaceFirst("(jdbc:postgresql://dpg-[a-z0-9]+-a)(:[0-9]+|/)", "$1.singapore-postgres.render.com$2");
        }
        if (url.startsWith("jdbc:postgresql://") && (url.contains("render.com") || url.contains("dpg-")) && !url.contains("sslmode")) {
            url = url.contains("?") ? url + "&sslmode=require" : url + "?sslmode=require";
        }
        return new ConnectionDetails(url, defaultUser, defaultPass);
    }

    public record ConnectionDetails(String jdbcUrl, String username, String password) {}

    private void migrateLegacyInstitutionColumns(java.sql.Connection conn) {
        try (java.sql.Statement stmt = conn.createStatement()) {
            // Drop obsolete trigger/function that referenced legacy institution_code
            try {
                stmt.execute("DROP FUNCTION IF EXISTS organization.sync_org_columns() CASCADE");
            } catch (Exception e) {
                log.debug("Note: Trigger sync_org_columns drop: {}", e.getMessage());
            }

            java.sql.DatabaseMetaData meta = conn.getMetaData();

            // If legacy 'code' column exists on PostgreSQL/H2, ensure it does not block inserts
            if (columnExists(meta, "organizations", "code")) {
                try {
                    stmt.execute("ALTER TABLE organization.organizations ALTER COLUMN code DROP NOT NULL");
                } catch (Exception ignored) {}
            }

            boolean hasInstCode = columnExists(meta, "organizations", "institution_code");
            boolean hasBankCode = columnExists(meta, "organizations", "bank_code");
            if (hasInstCode && !hasBankCode) {
                stmt.execute("ALTER TABLE organization.organizations RENAME COLUMN institution_code TO bank_code");
            } else if (hasInstCode && hasBankCode) {
                stmt.execute("UPDATE organization.organizations SET bank_code = COALESCE(bank_code, institution_code) WHERE bank_code IS NULL");
                stmt.execute("ALTER TABLE organization.organizations DROP COLUMN institution_code");
            }

            boolean hasInstName = columnExists(meta, "organizations", "institution_name");
            boolean hasBankName = columnExists(meta, "organizations", "bank_name");
            if (hasInstName && !hasBankName) {
                stmt.execute("ALTER TABLE organization.organizations RENAME COLUMN institution_name TO bank_name");
            } else if (hasInstName && hasBankName) {
                stmt.execute("UPDATE organization.organizations SET bank_name = COALESCE(bank_name, institution_name) WHERE bank_name IS NULL");
                stmt.execute("ALTER TABLE organization.organizations DROP COLUMN institution_name");
            }

            boolean hasInstType = columnExists(meta, "organizations", "institution_type");
            boolean hasBankType = columnExists(meta, "organizations", "bank_type");
            if (hasInstType && !hasBankType) {
                stmt.execute("ALTER TABLE organization.organizations RENAME COLUMN institution_type TO bank_type");
            } else if (hasInstType && hasBankType) {
                stmt.execute("UPDATE organization.organizations SET bank_type = COALESCE(bank_type, institution_type) WHERE bank_type IS NULL");
                stmt.execute("ALTER TABLE organization.organizations DROP COLUMN institution_type");
            }

            // Ensure cin, direct_clearing_number, micr_code, gst_no, license_number exist and don't conflict
            try {
                boolean hasCin = columnExists(meta, "organizations", "cin");
                boolean hasCinNumber = columnExists(meta, "organizations", "cin_number");
                if (hasCinNumber && !hasCin) {
                    stmt.execute("ALTER TABLE organization.organizations RENAME COLUMN cin_number TO cin");
                } else if (!hasCin) {
                    stmt.execute("ALTER TABLE organization.organizations ADD COLUMN IF NOT EXISTS cin VARCHAR(50)");
                }
                stmt.execute("ALTER TABLE organization.organizations ADD COLUMN IF NOT EXISTS cin_number VARCHAR(50)");
                stmt.execute("ALTER TABLE organization.organizations ADD COLUMN IF NOT EXISTS direct_clearing_number VARCHAR(50)");
                stmt.execute("ALTER TABLE organization.organizations ADD COLUMN IF NOT EXISTS micr_code VARCHAR(9)");
                stmt.execute("ALTER TABLE organization.organizations ADD COLUMN IF NOT EXISTS gst_no VARCHAR(50)");
                stmt.execute("ALTER TABLE organization.organizations ADD COLUMN IF NOT EXISTS license_number VARCHAR(100)");
            } catch (Exception e) {
                log.debug("Additional organization columns migration note: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.debug("Institution to bank column migration note: {}", e.getMessage());
        }
    }

    private boolean columnExists(java.sql.DatabaseMetaData meta, String tableName, String columnName) {
        try (java.sql.ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
            if (rs.next()) return true;
        } catch (Exception ignored) {}
        try (java.sql.ResultSet rs = meta.getColumns(null, "organization", tableName, columnName)) {
            if (rs.next()) return true;
        } catch (Exception ignored) {}
        try (java.sql.ResultSet rs = meta.getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
            if (rs.next()) return true;
        } catch (Exception ignored) {}
        return false;
    }
}
