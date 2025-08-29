package com.firefly.common.domain.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for JSON logging.
 * JSON logging is automatically enabled when logback-classic is on the classpath
 * through the logback-spring.xml configuration file.
 */
@AutoConfiguration
@Configuration
public class JsonLoggingAutoConfiguration {
    // JSON logging is configured via logback-spring.xml
    // No additional beans needed
}