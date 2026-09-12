package com.lyzer.lyzerrecime.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class AbstractPostgresTest {

    /**
     * The singleton container pattern: started once in a static initializer,
     * never stopped.
     *
     * NOT @Testcontainers + @Container. That combination is per-test-CLASS —
     * the JUnit extension starts the container in beforeAll and stops it in
     * afterAll, so every class extending this base pays full startup again. A
     * static initializer runs once per JVM, and Ryuk (Testcontainers' reaper
     * sidecar) removes the container at the end of the run, so the missing
     * stop() leaks nothing.
     *
     * @ServiceConnection still applies: it is processed by Boot's
     * ServiceConnectionContextCustomizerFactory, independently of the JUnit
     * extension.
     *
     * 14.17 exactly, matching compose.yaml: testing against a different
     * PostgreSQL version than you deploy defeats the purpose of using a real
     * database at all.
     */
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:14.17");

    static {
        POSTGRES.start();
    }
}
