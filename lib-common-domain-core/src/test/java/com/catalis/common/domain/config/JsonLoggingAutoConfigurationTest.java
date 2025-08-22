package com.catalis.common.domain.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class JsonLoggingAutoConfigurationTest {

    @Test
    void contextLoadsWithAutoConfiguration() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(JsonLoggingAutoConfiguration.class);
        try {
            assertNotNull(ctx);
        } finally {
            ctx.close();
        }
    }
}
