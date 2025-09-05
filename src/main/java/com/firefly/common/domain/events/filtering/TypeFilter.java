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

package com.firefly.common.domain.events.filtering;

import com.firefly.common.domain.events.DomainEventEnvelope;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Filter that accepts events based on event type matching.
 * Supports exact matching, wildcard patterns, and regular expressions.
 */
public class TypeFilter implements EventFilter {

    private final Set<String> exactTypes;
    private final Set<Pattern> patterns;
    private final FilterMode mode;

    public enum FilterMode {
        EXACT,      // Exact string matching
        WILDCARD,   // Simple wildcard matching (* and ?)
        REGEX       // Full regular expression matching
    }

    /**
     * Creates a filter for exact type matching.
     */
    public TypeFilter(String... types) {
        this.mode = FilterMode.EXACT;
        this.exactTypes = Set.of(types);
        this.patterns = Set.of();
    }

    /**
     * Creates a filter for exact type matching.
     */
    public TypeFilter(Collection<String> types) {
        this.mode = FilterMode.EXACT;
        this.exactTypes = new HashSet<>(types);
        this.patterns = Set.of();
    }

    /**
     * Creates a filter with specified mode and patterns.
     */
    public TypeFilter(FilterMode mode, String... patterns) {
        this.mode = mode;
        this.exactTypes = Set.of();
        this.patterns = compilePatterns(mode, patterns);
    }

    /**
     * Creates a filter with specified mode and patterns.
     */
    public TypeFilter(FilterMode mode, Collection<String> patterns) {
        this.mode = mode;
        this.exactTypes = Set.of();
        this.patterns = compilePatterns(mode, patterns);
    }

    @Override
    public boolean accept(DomainEventEnvelope envelope) {
        if (envelope.getType() == null) {
            return false;
        }

        if (mode == FilterMode.EXACT) {
            return exactTypes.contains(envelope.getType());
        }

        return patterns.stream().anyMatch(pattern -> pattern.matcher(envelope.getType()).matches());
    }

    @Override
    public String getDescription() {
        if (mode == FilterMode.EXACT) {
            return "TypeFilter(exact: " + exactTypes + ")";
        }
        return "TypeFilter(" + mode.name().toLowerCase() + ": " + patterns.size() + " patterns)";
    }

    private Set<Pattern> compilePatterns(FilterMode mode, String... patterns) {
        Set<Pattern> compiled = new HashSet<>();
        for (String pattern : patterns) {
            compiled.add(compilePattern(mode, pattern));
        }
        return compiled;
    }

    private Set<Pattern> compilePatterns(FilterMode mode, Collection<String> patterns) {
        Set<Pattern> compiled = new HashSet<>();
        for (String pattern : patterns) {
            compiled.add(compilePattern(mode, pattern));
        }
        return compiled;
    }

    private Pattern compilePattern(FilterMode mode, String pattern) {
        switch (mode) {
            case WILDCARD:
                // Convert wildcard to regex: * -> .*, ? -> .
                String regex = pattern
                    .replace(".", "\\.")
                    .replace("*", ".*")
                    .replace("?", ".");
                return Pattern.compile(regex);
            case REGEX:
                return Pattern.compile(pattern);
            default:
                throw new IllegalArgumentException("Unsupported mode for pattern compilation: " + mode);
        }
    }

    /**
     * Static factory methods for convenience.
     */
    public static TypeFilter exact(String... types) {
        return new TypeFilter(types);
    }

    public static TypeFilter exact(Collection<String> types) {
        return new TypeFilter(types);
    }

    public static TypeFilter wildcard(String... patterns) {
        return new TypeFilter(FilterMode.WILDCARD, patterns);
    }

    public static TypeFilter regex(String... patterns) {
        return new TypeFilter(FilterMode.REGEX, patterns);
    }

    /**
     * Convenience methods for common event type patterns.
     */
    public static TypeFilter created() {
        return wildcard("*.created");
    }

    public static TypeFilter updated() {
        return wildcard("*.updated");
    }

    public static TypeFilter deleted() {
        return wildcard("*.deleted");
    }

    public static TypeFilter crudOperations() {
        return wildcard("*.created", "*.updated", "*.deleted");
    }
}