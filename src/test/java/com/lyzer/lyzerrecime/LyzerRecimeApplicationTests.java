package com.lyzer.lyzerrecime;

import com.lyzer.lyzerrecime.support.AbstractPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Extends AbstractPostgresTest so the context is built against the shared
 * Testcontainers database. A bare @SpringBootTest falls through to
 * application.properties and demands a hand-started PostgreSQL on
 * localhost:5432 — a second, avoidable infrastructure dependency for CI.
 */
@SpringBootTest
@ActiveProfiles("test")
class LyzerRecimeApplicationTests extends AbstractPostgresTest {

    @Test
    void contextLoads() {
    }

}
