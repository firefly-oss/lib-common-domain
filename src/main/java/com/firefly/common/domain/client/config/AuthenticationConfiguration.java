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
package com.firefly.common.domain.client.config;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Configuration class for authentication mechanisms including Bearer tokens,
 * Basic authentication, API keys, and custom authentication headers.
 * 
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
public class AuthenticationConfiguration {
    
    private final AuthenticationType type;
    private final Map<String, String> headers;
    private final Supplier<String> tokenSupplier;
    private final String username;
    private final String password;
    private final String apiKeyHeader;
    private final String apiKeyValue;
    
    private AuthenticationConfiguration(Builder builder) {
        this.type = builder.type;
        this.headers = Map.copyOf(builder.headers);
        this.tokenSupplier = builder.tokenSupplier;
        this.username = builder.username;
        this.password = builder.password;
        this.apiKeyHeader = builder.apiKeyHeader;
        this.apiKeyValue = builder.apiKeyValue;
    }
    
    /**
     * Creates a new authentication configuration builder.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a Bearer token authentication configuration.
     *
     * @param token the bearer token
     * @return authentication configuration
     */
    public static AuthenticationConfiguration bearerToken(String token) {
        return builder().bearerToken(token).build();
    }
    
    /**
     * Creates a Bearer token authentication configuration with token supplier.
     *
     * @param tokenSupplier supplier that provides the bearer token
     * @return authentication configuration
     */
    public static AuthenticationConfiguration bearerToken(Supplier<String> tokenSupplier) {
        return builder().bearerToken(tokenSupplier).build();
    }
    
    /**
     * Creates a Basic authentication configuration.
     *
     * @param username the username
     * @param password the password
     * @return authentication configuration
     */
    public static AuthenticationConfiguration basicAuth(String username, String password) {
        return builder().basicAuth(username, password).build();
    }
    
    /**
     * Creates an API key authentication configuration.
     *
     * @param headerName the header name for the API key
     * @param apiKey the API key value
     * @return authentication configuration
     */
    public static AuthenticationConfiguration apiKey(String headerName, String apiKey) {
        return builder().apiKey(headerName, apiKey).build();
    }
    
    /**
     * Creates a custom authentication configuration with headers.
     *
     * @param headers custom authentication headers
     * @return authentication configuration
     */
    public static AuthenticationConfiguration custom(Map<String, String> headers) {
        return builder().customHeaders(headers).build();
    }
    
    /**
     * Gets the authentication type.
     *
     * @return the authentication type
     */
    public AuthenticationType getType() {
        return type;
    }
    
    /**
     * Gets the authentication headers.
     *
     * @return immutable map of authentication headers
     */
    public Map<String, String> getHeaders() {
        if (type == AuthenticationType.BEARER_TOKEN && tokenSupplier != null) {
            Map<String, String> dynamicHeaders = new HashMap<>(headers);
            dynamicHeaders.put("Authorization", "Bearer " + tokenSupplier.get());
            return dynamicHeaders;
        }
        return headers;
    }
    
    /**
     * Gets the token supplier for dynamic token authentication.
     *
     * @return the token supplier, or null if not applicable
     */
    public Supplier<String> getTokenSupplier() {
        return tokenSupplier;
    }
    
    /**
     * Checks if this configuration uses dynamic tokens.
     *
     * @return true if using dynamic token supplier
     */
    public boolean isDynamicToken() {
        return tokenSupplier != null;
    }
    
    /**
     * Authentication types supported by the configuration.
     */
    public enum AuthenticationType {
        NONE,
        BEARER_TOKEN,
        BASIC_AUTH,
        API_KEY,
        CUSTOM
    }
    
    /**
     * Builder for AuthenticationConfiguration.
     */
    public static class Builder {
        private AuthenticationType type = AuthenticationType.NONE;
        private final Map<String, String> headers = new HashMap<>();
        private Supplier<String> tokenSupplier;
        private String username;
        private String password;
        private String apiKeyHeader;
        private String apiKeyValue;
        
        /**
         * Configures Bearer token authentication.
         *
         * @param token the bearer token
         * @return this builder instance
         */
        public Builder bearerToken(String token) {
            if (token != null && !token.trim().isEmpty()) {
                this.type = AuthenticationType.BEARER_TOKEN;
                this.headers.put("Authorization", "Bearer " + token.trim());
                this.tokenSupplier = null;
            }
            return this;
        }
        
        /**
         * Configures Bearer token authentication with dynamic token supplier.
         *
         * @param tokenSupplier supplier that provides the bearer token
         * @return this builder instance
         */
        public Builder bearerToken(Supplier<String> tokenSupplier) {
            if (tokenSupplier != null) {
                this.type = AuthenticationType.BEARER_TOKEN;
                this.tokenSupplier = tokenSupplier;
                // Headers will be generated dynamically
            }
            return this;
        }
        
        /**
         * Configures Basic authentication.
         *
         * @param username the username
         * @param password the password
         * @return this builder instance
         */
        public Builder basicAuth(String username, String password) {
            if (username != null && password != null) {
                this.type = AuthenticationType.BASIC_AUTH;
                this.username = username;
                this.password = password;
                String credentials = Base64.getEncoder()
                    .encodeToString((username + ":" + password).getBytes());
                this.headers.put("Authorization", "Basic " + credentials);
            }
            return this;
        }
        
        /**
         * Configures API key authentication.
         *
         * @param headerName the header name for the API key
         * @param apiKey the API key value
         * @return this builder instance
         */
        public Builder apiKey(String headerName, String apiKey) {
            if (headerName != null && apiKey != null) {
                this.type = AuthenticationType.API_KEY;
                this.apiKeyHeader = headerName;
                this.apiKeyValue = apiKey;
                this.headers.put(headerName, apiKey);
            }
            return this;
        }
        
        /**
         * Configures custom authentication headers.
         *
         * @param headers custom authentication headers
         * @return this builder instance
         */
        public Builder customHeaders(Map<String, String> headers) {
            if (headers != null && !headers.isEmpty()) {
                this.type = AuthenticationType.CUSTOM;
                this.headers.putAll(headers);
            }
            return this;
        }
        
        /**
         * Adds a custom authentication header.
         *
         * @param name the header name
         * @param value the header value
         * @return this builder instance
         */
        public Builder customHeader(String name, String value) {
            if (name != null && value != null) {
                if (this.type == AuthenticationType.NONE) {
                    this.type = AuthenticationType.CUSTOM;
                }
                this.headers.put(name, value);
            }
            return this;
        }
        
        /**
         * Builds the AuthenticationConfiguration.
         *
         * @return the configured AuthenticationConfiguration
         */
        public AuthenticationConfiguration build() {
            return new AuthenticationConfiguration(this);
        }
    }
}
