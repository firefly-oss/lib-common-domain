package com.catalis.common.domain.stepevents;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class StepEventAdapterUtilsTest {

    @Test
    void isClassPresentWorks() {
        assertTrue(StepEventAdapterUtils.isClassPresent("java.lang.String"));
        assertFalse(StepEventAdapterUtils.isClassPresent("com.example.DoesNotExist_123"));
    }

    @Test
    void resolveBeanByNameThenByType() {
        ApplicationContext ctx = Mockito.mock(ApplicationContext.class);
        Object named = new Object();
        when(ctx.getBean("myBean")).thenReturn(named);
        assertSame(named, StepEventAdapterUtils.resolveBean(ctx, "myBean", "java.lang.Runnable"));

        // now resolve by type when bean name not provided
        ApplicationContext ctx2 = Mockito.mock(ApplicationContext.class);
        Runnable r = () -> {};
        when(ctx2.getBeansOfType((Class) any())).thenAnswer(inv -> {
            Class<?> type = inv.getArgument(0);
            if (type.equals(Runnable.class)) {
                return Map.of("r", r);
            }
            return Map.of();
        });
        Object resolved = StepEventAdapterUtils.resolveBean(ctx2, null, "java.lang.Runnable");
        assertSame(r, resolved);

        // returns null if nothing found
        Object none = StepEventAdapterUtils.resolveBean(ctx2, null, "com.example.MissingType");
        assertNull(none);
    }

    @Test
    void templateReplacesPlaceholders() {
        String tpl = "ex-${topic}-${type}-${key}";
        String out = StepEventAdapterUtils.template(tpl, "orders", null, "k1");
        assertEquals("ex-orders--k1", out);

        assertNull(StepEventAdapterUtils.template(null, "a", "b", "c"));
    }
}
