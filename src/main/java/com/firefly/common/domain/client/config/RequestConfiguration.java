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

import io.github.resilience4j.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for per-request settings that can override default client configuration.
 * Provides fluent API for configuring headers, query parameters, timeouts, and retry policies
 * on a per-request basis.
 * 
 * @author Firefly Software Solutions Inc
 * @since 1.0.0
 */
public class RequestConfiguration {
    
    private final Map<String, String> headers;
    private final Map<String, Object> queryParams;
    private final Duration timeout;
    private final Retry retry;
    private final String contentType;
    private final String acceptType;
    
    private RequestConfiguration(Builder builder) {
        this.headers = Collections.unmodifiableMap(new HashMap<>(builder.headers));
        this.queryParams = Collections.unmodifiableMap(new HashMap<>(builder.queryParams));
        this.timeout = builder.timeout;
        this.retry = builder.retry;
        this.contentType = builder.contentType;
        this.acceptType = builder.acceptType;
    }
    
    /**
     * Creates a new RequestConfiguration builder.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates an empty RequestConfiguration with no overrides.
     *
     * @return an empty configuration
     */
    public static RequestConfiguration empty() {
        return new Builder().build();
    }
    
    /**
     * Gets the headers for this request.
     *
     * @return immutable map of headers
     */
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    /**
     * Gets the query parameters for this request.
     *
     * @return immutable map of query parameters
     */
    public Map<String, Object> getQueryParams() {
        return queryParams;
    }
    
    /**
     * Gets the timeout for this request.
     *
     * @return the timeout duration, or null if not specified
     */
    public Duration getTimeout() {
        return timeout;
    }
    
    /**
     * Gets the retry policy for this request.
     *
     * @return the retry instance, or null if not specified
     */
    public Retry getRetry() {
        return retry;
    }
    
    /**
     * Gets the content type for this request.
     *
     * @return the content type, or null if not specified
     */
    public String getContentType() {
        return contentType;
    }
    
    /**
     * Gets the accept type for this request.
     *
     * @return the accept type, or null if not specified
     */
    public String getAcceptType() {
        return acceptType;
    }
    
    /**
     * Checks if this configuration has any overrides.
     *
     * @return true if any configuration is specified
     */
    public boolean hasOverrides() {
        return !headers.isEmpty() || !queryParams.isEmpty() || 
               timeout != null || retry != null || 
               contentType != null || acceptType != null;
    }
    
    /**
     * Builder for RequestConfiguration.
     */
    public static class Builder {
        private final Map<String, String> headers = new HashMap<>();
        private final Map<String, Object> queryParams = new HashMap<>();
        private Duration timeout;
        private Retry retry;
        private String contentType;
        private String acceptType;
        
        /**
         * Adds a header to the request.
         *
         * @param name the header name
         * @param value the header value
         * @return this builder instance
         */
        public Builder header(String name, String value) {
            if (name != null && value != null) {
                this.headers.put(name, value);
            }
            return this;
        }
        
        /**
         * Adds multiple headers to the request.
         *
         * @param headers map of headers to add
         * @return this builder instance
         */
        public Builder headers(Map<String, String> headers) {
            if (headers != null) {
                this.headers.putAll(headers);
            }
            return this;
        }
        
        /**
         * Adds a query parameter to the request.
         *
         * @param name the parameter name
         * @param value the parameter value
         * @return this builder instance
         */
        public Builder queryParam(String name, Object value) {
            if (name != null && value != null) {
                this.queryParams.put(name, value);
            }
            return this;
        }
        
        /**
         * Adds multiple query parameters to the request.
         *
         * @param queryParams map of query parameters to add
         * @return this builder instance
         */
        public Builder queryParams(Map<String, Object> queryParams) {
            if (queryParams != null) {
                this.queryParams.putAll(queryParams);
            }
            return this;
        }
        
        /**
         * Sets the timeout for this request.
         *
         * @param timeout the timeout duration
         * @return this builder instance
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }
        
        /**
         * Sets the retry policy for this request.
         *
         * @param retry the retry instance
         * @return this builder instance
         */
        public Builder retry(Retry retry) {
            this.retry = retry;
            return this;
        }
        
        /**
         * Sets the content type for this request.
         *
         * @param contentType the content type
         * @return this builder instance
         */
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }
        
        /**
         * Sets the accept type for this request.
         *
         * @param acceptType the accept type
         * @return this builder instance
         */
        public Builder acceptType(String acceptType) {
            this.acceptType = acceptType;
            return this;
        }
        
        /**
         * Sets the content type to application/json.
         *
         * @return this builder instance
         */
        public Builder jsonContent() {
            return contentType("application/json");
        }
        
        /**
         * Sets the accept type to application/json.
         *
         * @return this builder instance
         */
        public Builder acceptJson() {
            return acceptType("application/json");
        }
        
        /**
         * Sets both content type and accept type to application/json.
         *
         * @return this builder instance
         */
        public Builder json() {
            return jsonContent().acceptJson();
        }
        
        /**
         * Adds an Authorization header with Bearer token.
         *
         * @param token the bearer token
         * @return this builder instance
         */
        public Builder bearerToken(String token) {
            if (token != null) {
                return header("Authorization", "Bearer " + token);
            }
            return this;
        }
        
        /**
         * Adds an Authorization header with Basic authentication.
         *
         * @param username the username
         * @param password the password
         * @return this builder instance
         */
        public Builder basicAuth(String username, String password) {
            if (username != null && password != null) {
                String credentials = java.util.Base64.getEncoder()
                    .encodeToString((username + ":" + password).getBytes());
                return header("Authorization", "Basic " + credentials);
            }
            return this;
        }
        
        /**
         * Adds an API key header.
         *
         * @param headerName the header name for the API key
         * @param apiKey the API key value
         * @return this builder instance
         */
        public Builder apiKey(String headerName, String apiKey) {
            return header(headerName, apiKey);
        }
        
        /**
         * Builds the RequestConfiguration.
         *
         * @return the configured RequestConfiguration
         */
        public RequestConfiguration build() {
            return new RequestConfiguration(this);
        }
    }
}
