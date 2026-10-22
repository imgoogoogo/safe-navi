package com.safenavi.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/** Needs a MySQL with schema.sql applied (ddl-auto=validate), so it only runs when DB_URL is set. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class SafeNaviApplicationTests {

    @Test
    void contextLoads() {
    }

}
