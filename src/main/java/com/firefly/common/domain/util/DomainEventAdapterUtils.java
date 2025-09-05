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

package com.firefly.common.domain.util;

import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.Map;

public final class DomainEventAdapterUtils {

    private DomainEventAdapterUtils() {}

    public static boolean isClassPresent(String fqn) {
        try {
            Class.forName(fqn);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static Object resolveBean(ApplicationContext ctx, String beanName, String... candidateTypeFqns) {
        try {
            if (beanName != null && !beanName.isEmpty()) {
                return ctx.getBean(beanName);
            }
            for (String fqn : candidateTypeFqns) {
                try {
                    Class<?> type = Class.forName(fqn);
                    Map<String, ?> beans = ctx.getBeansOfType(type);
                    if (!beans.isEmpty()) {
                        return beans.values().iterator().next();
                    }
                } catch (ClassNotFoundException ignored) {
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static String template(String tpl, String topic, String type, String key) {
        if (tpl == null) return null;
        return tpl.replace("${topic}", nullToEmpty(topic))
                  .replace("${type}", nullToEmpty(type))
                  .replace("${key}", nullToEmpty(key));
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    public static void invokeVoid(Object target, String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = target.getClass().getMethod(methodName, paramTypes);
        m.invoke(target, args);
    }
}