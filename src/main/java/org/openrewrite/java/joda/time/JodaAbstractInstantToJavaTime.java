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
package org.openrewrite.java.joda.time;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

@Value
@EqualsAndHashCode(callSuper = false)
public class JodaAbstractInstantToJavaTime extends Recipe {
    String displayName = "Migrate Joda-Time `AbstractInstant` to Java time";

    String description = "Migrates Joda-Time `AbstractInstant` method calls to their Java time equivalents.";

    private static final MethodMatcher IS_AFTER_LONG = new MethodMatcher("org.joda.time.base.AbstractInstant isAfter(long)");
    private static final MethodMatcher IS_BEFORE_LONG = new MethodMatcher("org.joda.time.base.AbstractInstant isBefore(long)");
    private static final MethodMatcher IS_BEFORE_NOW = new MethodMatcher("org.joda.time.base.AbstractInstant isBeforeNow()");
    private static final MethodMatcher IS_EQUAL_LONG = new MethodMatcher("org.joda.time.base.AbstractInstant isEqual(long)");
    private static final MethodMatcher TO_DATE = new MethodMatcher("org.joda.time.base.AbstractInstant toDate()");
    private static final MethodMatcher TO_STRING_FORMATTER = new MethodMatcher("org.joda.time.base.AbstractInstant toString(org.joda.time.format.DateTimeFormatter)");
    private static final MethodMatcher TO_INSTANT = new MethodMatcher("org.joda.time.base.AbstractInstant toInstant()");
    private static final MethodMatcher GET_MILLIS = new MethodMatcher("org.joda.time.base.BaseDateTime getMillis()");


    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("org.joda.time.*", true), new JavaVisitor<ExecutionContext>() {
            private boolean isInstantType(@Nullable Expression select) {
                return select != null && TypeUtils.isOfClassType(select.getType(), "org.joda.time.Instant");
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (IS_AFTER_LONG.matches(method)) {
                    if (isInstantType(method.getSelect())) {
                        maybeAddImport("java.time.Instant");
                        return JavaTemplate.builder("#{any(java.time.Instant)}.isAfter(Instant.ofEpochMilli(#{any(long)}))")
                                .imports("java.time.Instant").build()
                                .apply(getCursor(), m.getCoordinates().replace(),
                                        m.getSelect(), m.getArguments().get(0));
                    }
                    maybeAddImport("java.time.Instant");
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.isAfter(Instant.ofEpochMilli(#{any(long)}).atZone(ZoneId.systemDefault()))")
                            .imports("java.time.Instant", "java.time.ZoneId").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                                    m.getSelect(), m.getArguments().get(0));
                }
                if (IS_BEFORE_LONG.matches(method)) {
                    if (isInstantType(method.getSelect())) {
                        maybeAddImport("java.time.Instant");
                        return JavaTemplate.builder("#{any(java.time.Instant)}.isBefore(Instant.ofEpochMilli(#{any(long)}))")
                                .imports("java.time.Instant").build()
                                .apply(getCursor(), m.getCoordinates().replace(),
                                        m.getSelect(), m.getArguments().get(0));
                    }
                    maybeAddImport("java.time.Instant");
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.isBefore(Instant.ofEpochMilli(#{any(long)}).atZone(ZoneId.systemDefault()))")
                            .imports("java.time.Instant", "java.time.ZoneId").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                                    m.getSelect(), m.getArguments().get(0));
                }
                if (IS_BEFORE_NOW.matches(method)) {
                    maybeAddImport("java.time.ZonedDateTime");
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.isBefore(ZonedDateTime.now())")
                            .imports("java.time.ZonedDateTime").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (IS_EQUAL_LONG.matches(method)) {
                    maybeAddImport("java.time.Instant");
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.isEqual(Instant.ofEpochMilli(#{any(long)}).atZone(ZoneId.systemDefault()))")
                            .imports("java.time.Instant", "java.time.ZoneId").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                                    m.getSelect(), m.getArguments().get(0));
                }
                if (TO_DATE.matches(method)) {
                    maybeAddImport("java.util.Date");
                    return JavaTemplate.builder("Date.from(#{any(java.time.ZonedDateTime)}.toInstant())")
                            .imports("java.util.Date").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (TO_STRING_FORMATTER.matches(method)) {
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.format(#{any(java.time.format.DateTimeFormatter)})")
                            .build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                                    m.getSelect(), m.getArguments().get(0));
                }
                if (TO_INSTANT.matches(method)) {
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.toInstant()").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (GET_MILLIS.matches(method)) {
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.toInstant().toEpochMilli()").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                return m;
            }
        });
    }
}
