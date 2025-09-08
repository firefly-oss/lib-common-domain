/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firefly.common.domain.config;

import com.firefly.common.domain.cqrs.command.CommandBus;
import com.firefly.common.domain.cqrs.command.DefaultCommandBus;
import com.firefly.common.domain.cqrs.query.DefaultQueryBus;
import com.firefly.common.domain.cqrs.query.QueryBus;
import com.firefly.common.domain.tracing.CorrelationContext;
import com.firefly.common.domain.validation.AutoValidationProcessor;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for CQRS framework components.
 * Provides automatic setup of CommandBus, QueryBus, and related infrastructure.
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(RedisCacheAutoConfiguration.class)
@EnableCaching
@EnableConfigurationProperties(CqrsProperties.class)
@ConditionalOnProperty(prefix = "firefly.cqrs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CqrsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AutoValidationProcessor autoValidationProcessor(@Autowired(required = false) Validator validator) {
        if (validator != null) {
            log.info("Configuring Jakarta validation processor for CQRS framework");
            return new AutoValidationProcessor(validator);
        } else {
            log.warn("Jakarta Validator not available - creating no-op validation processor");
            return new AutoValidationProcessor(null);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public io.micrometer.core.instrument.MeterRegistry meterRegistry() {
        log.info("Auto-configuring default SimpleMeterRegistry for CQRS metrics");
        return new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public CommandBus commandBus(ApplicationContext applicationContext,
                               CorrelationContext correlationContext,
                               AutoValidationProcessor autoValidationProcessor,
                               io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        log.info("Configuring CQRS Command Bus with Jakarta validation and metrics (auto-configured)");
        return new DefaultCommandBus(applicationContext, correlationContext, autoValidationProcessor, meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public QueryBus queryBus(ApplicationContext applicationContext,
                           CorrelationContext correlationContext,
                           AutoValidationProcessor autoValidationProcessor,
                           CacheManager cacheManager,
                           io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        log.info("Configuring CQRS Query Bus with caching support, Jakarta validation and metrics (auto-configured)");
        return new DefaultQueryBus(applicationContext, correlationContext, autoValidationProcessor, cacheManager, meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager localCacheManager(CqrsProperties cqrsProperties) {
        CqrsProperties.Cache.CacheType cacheType = cqrsProperties.getQuery().getCache().getType();

        if (cacheType == CqrsProperties.Cache.CacheType.REDIS &&
            cqrsProperties.getQuery().getCache().getRedis().isEnabled()) {
            log.warn("Redis cache is configured but Redis auto-configuration did not activate. " +
                    "Falling back to local cache. Check Redis dependencies and configuration.");
        }

        log.info("Configuring default local cache manager for CQRS queries");
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(java.util.Arrays.asList("query-cache"));
        return cacheManager;
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheConfigurationValidator cacheConfigurationValidator(CqrsProperties cqrsProperties) {
        return new CacheConfigurationValidator(cqrsProperties);
    }
}