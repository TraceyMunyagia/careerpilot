package com.careerpilot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Starts the WHOLE application, including the database connection.
 * Requires the Docker database to be running: docker compose up -d
 */
@SpringBootTest
class CareerPilotApplicationTests {

    @Test
    void contextLoads() {
        // Passes if every bean can be created and wired together
    }
}