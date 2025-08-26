package com.catalis.common.domain.events.inbound;

import com.catalis.common.domain.events.DomainEventEnvelope;
import com.catalis.common.domain.events.DomainSpringEvent;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple in-process dispatcher that routes DomainSpringEvent to methods annotated with @EventListener.
 */
public class EventListenerDispatcher implements ApplicationListener<DomainSpringEvent>, ApplicationContextAware, InitializingBean {

    private ApplicationContext applicationContext;
    private final List<Handler> handlers = new ArrayList<>();
    private volatile boolean initialized = false;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        // Do nothing here to avoid circular dependency
        // Handlers will be initialized lazily on first event
    }

    /**
     * Lazily initialize handlers to avoid circular dependency issues
     */
    private void initializeHandlers() {
        if (!initialized && applicationContext != null) {
            synchronized (this) {
                if (!initialized) {
                    String[] beanNames = applicationContext.getBeanDefinitionNames();
                    for (String name : beanNames) {
                        try {
                            // Use getBean without forcing eager initialization
                            if (applicationContext.containsBean(name)) {
                                Object bean = applicationContext.getBean(name);
                                for (Method m : bean.getClass().getMethods()) {
                                    EventListener ann = m.getAnnotation(EventListener.class);
                                    if (ann != null) {
                                        handlers.add(new Handler(bean, m, ann.topic(), ann.type()));
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Skip beans that can't be instantiated yet
                            // This avoids circular dependency issues
                        }
                    }
                    initialized = true;
                }
            }
        }
    }

    @Override
    public void onApplicationEvent(DomainSpringEvent event) {
        // Initialize handlers lazily on first event to avoid circular dependency
        initializeHandlers();
        
        DomainEventEnvelope e = event.getEnvelope();
        for (Handler h : handlers) {
            if (matches(h.topic, e.topic) && matches(h.type, e.type)) {
                invoke(h, e);
            }
        }
    }

    private boolean matches(String pattern, String value) {
        if (pattern == null || pattern.isEmpty() || pattern.equals("*")) return true;
        return pattern.equals(value);
    }

    private void invoke(Handler h, DomainEventEnvelope e) {
        try {
            Class<?>[] paramTypes = h.method.getParameterTypes();
            if (paramTypes.length == 1) {
                Object arg;
                if (paramTypes[0].isAssignableFrom(DomainEventEnvelope.class)) {
                    arg = e;
                } else if (e.payload != null && paramTypes[0].isInstance(e.payload)) {
                    arg = e.payload;
                } else if (e.payload instanceof String s) {
                    Object mapper = tryGetObjectMapper();
                    if (mapper != null) {
                        Method read = mapper.getClass().getMethod("readValue", String.class, Class.class);
                        arg = read.invoke(mapper, s, paramTypes[0]);
                    } else {
                        arg = s;
                    }
                } else {
                    arg = e.payload;
                }
                h.method.invoke(h.bean, arg);
            } else if (paramTypes.length == 0) {
                h.method.invoke(h.bean);
            }
        } catch (Exception ignored) {
        }
    }

    private Object tryGetObjectMapper() {
        try {
            return applicationContext.getBean(Class.forName("com.fasterxml.jackson.databind.ObjectMapper"));
        } catch (Throwable t) {
            return null;
        }
    }

    private record Handler(Object bean, Method method, String topic, String type) {}
}