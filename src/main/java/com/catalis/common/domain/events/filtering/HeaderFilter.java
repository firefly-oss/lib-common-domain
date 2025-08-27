package com.catalis.common.domain.events.filtering;

import com.catalis.common.domain.events.DomainEventEnvelope;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Filter that accepts events based on header values.
 * Supports various matching strategies for header-based filtering.
 */
public class HeaderFilter implements EventFilter {

    private final String headerName;
    private final Predicate<Object> headerPredicate;
    private final String description;

    private HeaderFilter(String headerName, Predicate<Object> headerPredicate, String description) {
        this.headerName = Objects.requireNonNull(headerName, "Header name cannot be null");
        this.headerPredicate = Objects.requireNonNull(headerPredicate, "Header predicate cannot be null");
        this.description = description;
    }

    @Override
    public boolean accept(DomainEventEnvelope envelope) {
        if (envelope.getHeaders() == null || envelope.getHeaders().isEmpty()) {
            return false;
        }
        
        Object headerValue = envelope.getHeaders().get(headerName);
        return headerPredicate.test(headerValue);
    }

    @Override
    public String getDescription() {
        return "HeaderFilter(" + headerName + ": " + description + ")";
    }

    /**
     * Creates a filter that checks if the specified header exists.
     */
    public static HeaderFilter exists(String headerName) {
        return new HeaderFilter(headerName, Objects::nonNull, "exists");
    }

    /**
     * Creates a filter that checks if the specified header does not exist.
     */
    public static HeaderFilter notExists(String headerName) {
        return new HeaderFilter(headerName, Objects::isNull, "not exists");
    }

    /**
     * Creates a filter that checks if the header value equals the specified value.
     */
    public static HeaderFilter equals(String headerName, Object expectedValue) {
        return new HeaderFilter(headerName, 
            value -> Objects.equals(value, expectedValue),
            "equals " + expectedValue);
    }

    /**
     * Creates a filter that checks if the header value (as string) equals the specified string.
     */
    public static HeaderFilter equalsString(String headerName, String expectedValue) {
        return new HeaderFilter(headerName,
            value -> value != null && Objects.equals(value.toString(), expectedValue),
            "equals '" + expectedValue + "'");
    }

    /**
     * Creates a filter that checks if the header value (as string) contains the specified substring.
     */
    public static HeaderFilter contains(String headerName, String substring) {
        return new HeaderFilter(headerName,
            value -> value != null && value.toString().contains(substring),
            "contains '" + substring + "'");
    }

    /**
     * Creates a filter that checks if the header value (as string) starts with the specified prefix.
     */
    public static HeaderFilter startsWith(String headerName, String prefix) {
        return new HeaderFilter(headerName,
            value -> value != null && value.toString().startsWith(prefix),
            "starts with '" + prefix + "'");
    }

    /**
     * Creates a filter that checks if the header value (as string) ends with the specified suffix.
     */
    public static HeaderFilter endsWith(String headerName, String suffix) {
        return new HeaderFilter(headerName,
            value -> value != null && value.toString().endsWith(suffix),
            "ends with '" + suffix + "'");
    }

    /**
     * Creates a filter that checks if the header value (as string) matches the specified regex pattern.
     */
    public static HeaderFilter matches(String headerName, String regex) {
        Pattern pattern = Pattern.compile(regex);
        return new HeaderFilter(headerName,
            value -> value != null && pattern.matcher(value.toString()).matches(),
            "matches /" + regex + "/");
    }

    /**
     * Creates a filter that checks if the header value (as string) matches the specified pattern.
     */
    public static HeaderFilter matches(String headerName, Pattern pattern) {
        return new HeaderFilter(headerName,
            value -> value != null && pattern.matcher(value.toString()).matches(),
            "matches " + pattern.pattern());
    }

    /**
     * Creates a filter that checks if the header value is in the specified array of values.
     */
    public static HeaderFilter in(String headerName, Object... values) {
        return new HeaderFilter(headerName,
            value -> {
                for (Object expectedValue : values) {
                    if (Objects.equals(value, expectedValue)) {
                        return true;
                    }
                }
                return false;
            },
            "in [" + String.join(", ", java.util.Arrays.stream(values).map(String::valueOf).toArray(String[]::new)) + "]");
    }

    /**
     * Creates a filter using a custom predicate.
     */
    public static HeaderFilter custom(String headerName, Predicate<Object> predicate, String description) {
        return new HeaderFilter(headerName, predicate, description);
    }

    /**
     * Convenience methods for common headers.
     */
    public static HeaderFilter correlationId(String expectedId) {
        return equalsString("correlation-id", expectedId);
    }

    public static HeaderFilter correlationIdExists() {
        return exists("correlation-id");
    }

    public static HeaderFilter source(String expectedSource) {
        return equalsString("source", expectedSource);
    }

    public static HeaderFilter version(String expectedVersion) {
        return equalsString("version", expectedVersion);
    }

    /**
     * Creates a filter that accepts events with any of the specified header conditions.
     */
    public static HeaderFilter anyHeader(Map<String, Object> requiredHeaders) {
        return custom("multiple", 
            envelope -> {
                if (envelope instanceof DomainEventEnvelope) {
                    DomainEventEnvelope env = (DomainEventEnvelope) envelope;
                    if (env.getHeaders() == null) return false;
                    
                    return requiredHeaders.entrySet().stream()
                        .anyMatch(entry -> Objects.equals(env.getHeaders().get(entry.getKey()), entry.getValue()));
                }
                return false;
            },
            "any of " + requiredHeaders);
    }

    /**
     * Creates a filter that accepts events with all of the specified header conditions.
     */
    public static HeaderFilter allHeaders(Map<String, Object> requiredHeaders) {
        return custom("multiple",
            envelope -> {
                if (envelope instanceof DomainEventEnvelope) {
                    DomainEventEnvelope env = (DomainEventEnvelope) envelope;
                    if (env.getHeaders() == null) return false;
                    
                    return requiredHeaders.entrySet().stream()
                        .allMatch(entry -> Objects.equals(env.getHeaders().get(entry.getKey()), entry.getValue()));
                }
                return false;
            },
            "all of " + requiredHeaders);
    }
}