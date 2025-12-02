/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.joda.time.templates;

import org.openrewrite.java.JavaFieldTemplate;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.MethodCall;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.openrewrite.java.joda.time.templates.TimeClassNames.*;

public class AllTemplates {
    public static final Map<String, Templates> templates = new HashMap<String, Templates>() {
        {
            put(JODA_ABSTRACT_DATE_TIME, new AbstractDateTimeTemplates());
            put(JODA_ABSTRACT_DURATION, new AbstractDurationTemplates());
            put(JODA_ABSTRACT_INSTANT, new AbstractInstantTemplates());
            put(JODA_BASE_DATE_TIME, new BaseDateTime());
            put(JODA_TIME_FORMAT, new DateTimeFormatTemplates());
            put(JODA_TIME_FORMATTER, new DateTimeFormatterTemplates());
            put(JODA_DATE_TIME, new DateTimeTemplates());
            put(JODA_DURATION, new DurationTemplates());
            put(JODA_BASE_DURATION, new BaseDurationTemplates());
            put(JODA_DATE_TIME_ZONE, new DateTimeZoneTemplates());
            put(JODA_INSTANT, new InstantTemplates());
            put(JODA_INTERVAL, new IntervalTemplates());
            put(JODA_ABSTRACT_INTERVAL, new AbstractIntervalTemplates());
            put(JODA_BASE_INTERVAL, new BaseIntervalTemplates());
            put(JODA_DATE_TIME_MIDNIGHT, new DateTimeMidnightTemplates());
        }
    };

    public static JavaFieldTemplate getFieldTemplate(J.FieldAccess filed) {
        return getTemplateGroup(filed)
                .flatMap(t -> t.getFields().stream()
                        .filter(f -> f.getMatcher().match(filed))
                        .findFirst())
                .map(FieldTemplate::getTemplate)
                .orElse(null);
    }

    public static MethodTemplate getTemplate(MethodCall method) {
        return getTemplateGroup(method)
                .flatMap(templates -> templates.getTemplates().stream()
                        .filter(template -> template.getMatcher().matches(method) && templates.matchesMethodCall(method, template))
                        .findFirst())
                .orElse(null);
    }

    private static Optional<Templates> getTemplateGroup(Expression expression) {
        JavaType.Class type;
        if(expression instanceof J.MethodInvocation)
            type = (JavaType.Class)((J.MethodInvocation) expression).getMethodType().getDeclaringType();
        else
            type = (JavaType.Class) expression.getType();

        return Optional.ofNullable(templates.get(type.getFullyQualifiedName()));
    }
}
