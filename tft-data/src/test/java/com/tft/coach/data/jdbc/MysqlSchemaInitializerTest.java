package com.tft.coach.data.jdbc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MysqlSchemaInitializerTest {

    @Test
    void splitsBundledScriptIntoExecutableStatements() throws Exception {
        String script = MysqlSchemaInitializer.loadScript("db/mysql/V1__p1_stores.sql");
        String[] statements = MysqlSchemaInitializer.splitStatements(script);

        assertTrue(statements.length >= 4);
        assertEquals("CREATE TABLE IF NOT EXISTS evidence", statements[0].substring(0, 35));
    }
}
