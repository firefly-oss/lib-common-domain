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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
@EnableCaching
@EnableConfigurationProperties(CqrsProperties.class)
@ConditionalOnProperty(prefix = "firefly.cqrs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CqrsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CommandBus commandBus(ApplicationContext applicationContext,
                               CorrelationContext correlationContext) {
        log.info("Configuring CQRS Command Bus");
        return new DefaultCommandBus(applicationContext, correlationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public QueryBus queryBus(ApplicationContext applicationContext,
                           CorrelationContext correlationContext,
                           CacheManager cacheManager) {
        log.info("Configuring CQRS Query Bus with caching support");
        return new DefaultQueryBus(applicationContext, correlationContext, cacheManager);
    }

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager() {
        log.info("Configuring default cache manager for CQRS queries");
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(java.util.Arrays.asList("query-cache"));
        return cacheManager;
    }
}