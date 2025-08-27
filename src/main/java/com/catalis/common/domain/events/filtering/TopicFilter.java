package com.catalis.common.domain.events.filtering;

import com.catalis.common.domain.events.DomainEventEnvelope;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Filter that accepts events based on topic matching.
 * Supports exact matching, wildcard patterns, and regular expressions.
 */
public class TopicFilter implements EventFilter {

    private final Set<String> exactTopics;
    private final Set<Pattern> patterns;
    private final FilterMode mode;

    public enum FilterMode {
        EXACT,      // Exact string matching
        WILDCARD,   // Simple wildcard matching (* and ?)
        REGEX       // Full regular expression matching
    }

    /**
     * Creates a filter for exact topic matching.
     */
    public TopicFilter(String... topics) {
        this.mode = FilterMode.EXACT;
        this.exactTopics = Set.of(topics);
        this.patterns = Set.of();
    }

    /**
     * Creates a filter for exact topic matching.
     */
    public TopicFilter(Collection<String> topics) {
        this.mode = FilterMode.EXACT;
        this.exactTopics = new HashSet<>(topics);
        this.patterns = Set.of();
    }

    /**
     * Creates a filter with specified mode and patterns.
     */
    public TopicFilter(FilterMode mode, String... patterns) {
        this.mode = mode;
        this.exactTopics = Set.of();
        this.patterns = compilePatterns(mode, patterns);
    }

    /**
     * Creates a filter with specified mode and patterns.
     */
    public TopicFilter(FilterMode mode, Collection<String> patterns) {
        this.mode = mode;
        this.exactTopics = Set.of();
        this.patterns = compilePatterns(mode, patterns);
    }

    @Override
    public boolean accept(DomainEventEnvelope envelope) {
        if (envelope.getTopic() == null) {
            return false;
        }

        if (mode == FilterMode.EXACT) {
            return exactTopics.contains(envelope.getTopic());
        }

        return patterns.stream().anyMatch(pattern -> pattern.matcher(envelope.getTopic()).matches());
    }

    @Override
    public String getDescription() {
        if (mode == FilterMode.EXACT) {
            return "TopicFilter(exact: " + exactTopics + ")";
        }
        return "TopicFilter(" + mode.name().toLowerCase() + ": " + patterns.size() + " patterns)";
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
    public static TopicFilter exact(String... topics) {
        return new TopicFilter(topics);
    }

    public static TopicFilter exact(Collection<String> topics) {
        return new TopicFilter(topics);
    }

    public static TopicFilter wildcard(String... patterns) {
        return new TopicFilter(FilterMode.WILDCARD, patterns);
    }

    public static TopicFilter regex(String... patterns) {
        return new TopicFilter(FilterMode.REGEX, patterns);
    }
}