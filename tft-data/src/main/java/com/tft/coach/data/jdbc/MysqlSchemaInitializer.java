package com.tft.coach.data.jdbc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

import javax.sql.DataSource;

/** Applies bundled MySQL DDL when P1 store tables are missing. */
public final class MysqlSchemaInitializer {

    private static final String SCHEMA_RESOURCE = "db/mysql/V1__p1_stores.sql";
    private static final String PROBE_TABLE = "evidence";

    private final DataSource dataSource;

    public MysqlSchemaInitializer(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    /** Runs {@code V1__p1_stores.sql} when the evidence table does not exist. */
    public void initializeIfNeeded() throws SQLException {
        if (tableExists(PROBE_TABLE)) {
            return;
        }
        executeScript(loadScript(SCHEMA_RESOURCE));
    }

    private boolean tableExists(String tableName) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             ResultSet rs = connection.getMetaData().getTables(
                     connection.getCatalog(),
                     null,
                     tableName,
                     new String[] {"TABLE"})) {
            return rs.next();
        }
    }

    private void executeScript(String script) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : splitStatements(script)) {
                statement.execute(sql);
            }
        }
    }

    static String loadScript(String resourcePath) throws SQLException {
        InputStream in = MysqlSchemaInitializer.class.getClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            throw new SQLException("Missing schema resource: " + resourcePath);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (IOException ex) {
            throw new SQLException("Failed to read schema resource: " + resourcePath, ex);
        }
    }

    static String[] splitStatements(String script) {
        return script.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("--"))
                .reduce("", (acc, line) -> acc + line + " ")
                .trim()
                .replaceAll(";\\s*$", "")
                .split(";\\s*");
    }
}
