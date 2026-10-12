package org.example.backend.database;

import org.flywaydb.core.Flyway;

public final class CoreDatabaseMigrationRunner {

    private CoreDatabaseMigrationRunner() {
    }

    public static void main(String[] args) {
        Flyway.configure()
                .dataSource(required("SMART_LAB_DB_URL"), required("SMART_LAB_DB_USERNAME"), required("SMART_LAB_DB_PASSWORD"))
                .baselineOnMigrate(true)
                .baselineVersion("20261009")
                .locations("classpath:db/migration")
                .validateOnMigrate(true)
                .cleanDisabled(true)
                .target(required("SMART_LAB_FLYWAY_TARGET"))
                .load()
                .migrate();
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be configured in the process environment");
        }
        return value;
    }
}
