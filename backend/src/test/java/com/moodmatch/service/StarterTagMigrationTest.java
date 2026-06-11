package com.moodmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class StarterTagMigrationTest {

    @Test
    void shouldSeedStarterTagsIntoFreshDatabase() throws SQLException {
        String jdbcUrl =
                "jdbc:h2:mem:starter-tags;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";

        Flyway.configure()
                .dataSource(jdbcUrl, "sa", "sa")
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "sa");
                Statement statement = connection.createStatement()) {
            assertEquals(23, countRows(statement, "SELECT COUNT(*) FROM tags"));
            assertEquals(5, countRows(statement, "SELECT COUNT(*) FROM tags WHERE category = 'GENRE'"));
            assertEquals(5, countRows(statement, "SELECT COUNT(*) FROM tags WHERE category = 'THEME'"));
            assertEquals(5, countRows(statement, "SELECT COUNT(*) FROM tags WHERE category = 'TONE'"));
            assertEquals(4, countRows(statement, "SELECT COUNT(*) FROM tags WHERE category = 'SETTING'"));
            assertEquals(4, countRows(statement, "SELECT COUNT(*) FROM tags WHERE category = 'EXPERIENCE'"));
            assertTrue(exists(statement, "SELECT 1 FROM tags WHERE name = 'Sci-Fi' AND category = 'GENRE'"));
            assertTrue(exists(statement, "SELECT 1 FROM tags WHERE name = 'Space' AND category = 'THEME'"));
            assertTrue(exists(statement, "SELECT 1 FROM tags WHERE name = 'Thoughtful' AND category = 'TONE'"));
            assertTrue(exists(statement, "SELECT 1 FROM tags WHERE name = 'Future' AND category = 'SETTING'"));
            assertTrue(exists(statement, "SELECT 1 FROM tags WHERE name = 'Relaxing' AND category = 'EXPERIENCE'"));
        }
    }

    private int countRows(Statement statement, String sql) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private boolean exists(Statement statement, String sql) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next();
        }
    }
}
