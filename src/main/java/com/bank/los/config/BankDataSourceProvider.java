package com.bank.los.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic DataSource Provider for Multi-Bank databases.
 */
@Slf4j
@Component
public class BankDataSourceProvider {

    @Value("${bank.datasource.driver-class-name:${organization.datasource.driver-class-name:${tenant.datasource.driver-class-name:org.postgresql.Driver}}}")
    private String driverClassName;

    @Value("${bank.datasource.username:${organization.datasource.username:${tenant.datasource.username:postgres}}}")
    private String defaultUsername;

    @Value("${bank.datasource.password:${organization.datasource.password:${tenant.datasource.password:postgres}}}")
    private String defaultPassword;

    @Value("${bank.datasource.default-host:${organization.datasource.default-host:${tenant.datasource.default-host:localhost}}}")
    private String defaultHost;

    @Value("${bank.datasource.default-port:${organization.datasource.default-port:${tenant.datasource.default-port:5432}}}")
    private int defaultPort;

    @Value("${bank.datasource.default-url-prefix:${organization.datasource.default-url-prefix:${tenant.datasource.default-url-prefix:}}}")
    private String urlPrefix;

    @Value("${bank.datasource.default-url-suffix:${organization.datasource.default-url-suffix:${tenant.datasource.default-url-suffix:}}}")
    private String urlSuffix;

    @Value("${master.datasource.url:jdbc:postgresql://localhost:5432/los_master_db}")
    private String masterUrl;

    private final Map<String, DataSource> bankDataSources = new ConcurrentHashMap<>();

    public DataSource getOrCreateBankDataSource(String dbName, String dbHost, Integer dbPort) {
        return bankDataSources.computeIfAbsent(dbName, key -> createDataSource(dbName, dbHost, dbPort));
    }

    public DataSource getBankDataSource(String dbName) {
        return bankDataSources.get(dbName);
    }

    public void registerDataSource(String dbName, DataSource dataSource) {
        bankDataSources.put(dbName, dataSource);
    }

    public Map<String, DataSource> getAllBankDataSources() {
        return bankDataSources;
    }

    // Backwards-compatible aliases
    public DataSource getOrCreateOrganizationDataSource(String dbName, String dbHost, Integer dbPort) {
        return getOrCreateBankDataSource(dbName, dbHost, dbPort);
    }

    public DataSource getOrganizationDataSource(String dbName) {
        return getBankDataSource(dbName);
    }

    public Map<String, DataSource> getAllOrganizationDataSources() {
        return getAllBankDataSources();
    }

    public DataSource getOrCreateTenantDataSource(String dbName, String dbHost, Integer dbPort) {
        return getOrCreateBankDataSource(dbName, dbHost, dbPort);
    }

    public DataSource getTenantDataSource(String dbName) {
        return getBankDataSource(dbName);
    }

    public Map<String, DataSource> getAllTenantDataSources() {
        return getAllBankDataSources();
    }

    private DataSource createDataSource(String dbName, String dbHost, Integer dbPort) {
        log.info("Creating dynamic HikariDataSource for bank DB: {}", dbName);

        String jdbcUrl;
        if (urlPrefix != null && !urlPrefix.isEmpty()) {
            jdbcUrl = urlPrefix + dbName + (urlSuffix != null ? urlSuffix : "");
        } else {
            ensurePostgreSqlDatabaseExists(masterUrl, dbName, defaultUsername, defaultPassword);
            String host = (dbHost != null && !dbHost.isEmpty()) ? dbHost : defaultHost;
            int port = (dbPort != null && dbPort > 0) ? dbPort : defaultPort;
            jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName);
        }

        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName(driverClassName);

        PersistenceConfig.ConnectionDetails details = PersistenceConfig.parseConnectionDetails(jdbcUrl, defaultUsername, defaultPassword);
        ds.setJdbcUrl(details.jdbcUrl());
        ds.setUsername(details.username());
        ds.setPassword(details.password());
        ds.setPoolName("HikariPool-" + dbName);
        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(2);
        ds.setIdleTimeout(30000);
        ds.setConnectionTimeout(20000);

        try (Connection conn = ds.getConnection()) {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            ClassPathResource bankSchema = new ClassPathResource("db/bank-schema.sql");
            if (bankSchema.exists()) {
                populator.addScript(bankSchema);
            }
            ClassPathResource tenantSchema = new ClassPathResource("db/tenant-schema.sql");
            if (tenantSchema.exists()) {
                populator.addScript(tenantSchema);
            }
            ClassPathResource orgSchema = new ClassPathResource("db/organization-schema.sql");
            if (orgSchema.exists()) {
                populator.addScript(orgSchema);
            }
            populator.setContinueOnError(true);
            populator.setIgnoreFailedDrops(true);
            populator.populate(conn);
            migrateLegacyLeadColumns(conn);
        } catch (Exception e) {
            log.debug("Bank DB schema populate notice for {}: {}", dbName, e.getMessage());
        }

        return ds;
    }

    private void migrateLegacyLeadColumns(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            java.sql.DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, "customer", "leads", new String[]{"TABLE"})) {
                if (rs.next()) {
                    try { stmt.execute("ALTER TABLE customer.leads ALTER COLUMN created_at DROP NOT NULL"); } catch (Exception ignored) {}
                    try { stmt.execute("ALTER TABLE customer.leads ALTER COLUMN created_at SET DEFAULT now()"); } catch (Exception ignored) {}
                    try { stmt.execute("ALTER TABLE customer.leads ALTER COLUMN updated_at DROP NOT NULL"); } catch (Exception ignored) {}
                    try { stmt.execute("ALTER TABLE customer.leads ALTER COLUMN updated_at SET DEFAULT now()"); } catch (Exception ignored) {}
                    try { stmt.execute("ALTER TABLE customer.leads ALTER COLUMN customer_name DROP NOT NULL"); } catch (Exception ignored) {}
                    try { stmt.execute("ALTER TABLE customer.leads ALTER COLUMN phone DROP NOT NULL"); } catch (Exception ignored) {}
                    try { stmt.execute("ALTER TABLE customer.leads ALTER COLUMN lead_number DROP NOT NULL"); } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            log.debug("Note: lead columns migration notice: {}", e.getMessage());
        }
    }

    private void ensurePostgreSqlDatabaseExists(String masterDbUrl, String targetDbName, String username, String password) {
        if (targetDbName == null || targetDbName.isEmpty() || (urlPrefix != null && !urlPrefix.isEmpty())) {
            return;
        }
        try {
            PersistenceConfig.ConnectionDetails masterDetails = PersistenceConfig.parseConnectionDetails(masterDbUrl, username, password);
            try (Connection conn = DriverManager.getConnection(
                    masterDetails.jdbcUrl(), masterDetails.username(), masterDetails.password())) {
                conn.setAutoCommit(true);
                try (Statement checkStmt = conn.createStatement();
                     ResultSet rs = checkStmt.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + targetDbName + "'")) {
                    if (!rs.next()) {
                        log.info("Auto-provisioning bank database in PostgreSQL: {}", targetDbName);
                        try (Statement createStmt = conn.createStatement()) {
                            createStmt.executeUpdate("CREATE DATABASE \"" + targetDbName + "\"");
                            log.info("Successfully provisioned database: {}", targetDbName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Auto database creation note for {}: {}", targetDbName, e.getMessage());
        }
    }
}
