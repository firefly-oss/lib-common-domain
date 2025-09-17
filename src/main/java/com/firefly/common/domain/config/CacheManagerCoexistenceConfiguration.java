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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Configuration to ensure proper coexistence of cache managers from lib-common-domain and lib-common-web.
 * 
 * When both libraries are used together, this configuration ensures that:
 * 1. The idempotency cache manager (from lib-common-web) becomes the primary cache manager
 * 2. The CQRS cache manager (from lib-common-domain) remains available for internal CQRS use
 * 3. Spring's global @Cacheable infrastructure uses the idempotency cache manager as default
 * 
 * This resolves the "No qualifying bean of type 'CacheManager' available: expected single matching bean 
 * but found 2" error when both libraries are present.
 */
@Slf4j
@AutoConfiguration
@ConditionalOnBean(name = {"cqrsCacheManager", "idempotencyCacheManager"})
public class CacheManagerCoexistenceConfiguration {

    /**
     * Creates a primary cache manager bean that delegates to the idempotency cache manager.
     * This ensures that Spring's global caching infrastructure (@Cacheable, @CacheEvict, etc.)
     * uses the idempotency cache manager as the default, while keeping both cache managers available.
     * 
     * @param idempotencyCacheManager the cache manager from lib-common-web
     * @return the primary cache manager for global use
     */
    @Bean
    @Primary
    public CacheManager primaryCacheManager(@Qualifier("idempotencyCacheManager") CacheManager idempotencyCacheManager) {
        log.info("Detected both CQRS and Idempotency cache managers. Configuring idempotency cache manager as primary for global @Cacheable support.");
        log.info("CQRS cache manager (cqrsCacheManager) remains available for internal CQRS framework use.");
        
        // Return the idempotency cache manager as the primary one
        // This ensures that Spring's global caching uses the idempotency cache manager
        // while our CQRS framework continues to use its specific cache manager
        return idempotencyCacheManager;
    }
}
