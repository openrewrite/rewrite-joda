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
public class JodaInstantToJavaTime extends Recipe {
    String displayName = "Migrate Joda-Time `Instant` to Java time";

    String description = "Migrates `org.joda.time.Instant` constructors and methods to `java.time.Instant`.";

    private static final MethodMatcher CONSTRUCTOR = new MethodMatcher("org.joda.time.Instant <constructor>()");
    private static final MethodMatcher CONSTRUCTOR_LONG = new MethodMatcher("org.joda.time.Instant <constructor>(long)");
    private static final MethodMatcher CONSTRUCTOR_OBJECT = new MethodMatcher("org.joda.time.Instant <constructor>(Object)");
    private static final MethodMatcher PLUS_LONG = new MethodMatcher("org.joda.time.Instant plus(long)");
    private static final MethodMatcher MINUS_LONG = new MethodMatcher("org.joda.time.Instant minus(long)");
    private static final MethodMatcher WITH_MILLIS = new MethodMatcher("org.joda.time.Instant withMillis(long)");
    private static final MethodMatcher WITH_DURATION_ADDED_LONG = new MethodMatcher("org.joda.time.Instant withDurationAdded(long, int)");
    private static final MethodMatcher WITH_DURATION_ADDED = new MethodMatcher("org.joda.time.Instant withDurationAdded(org.joda.time.ReadableDuration, int)");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("org.joda.time.Instant", true), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass nc = (J.NewClass) super.visitNewClass(newClass, ctx);
                if (CONSTRUCTOR.matches(newClass)) {
                    return replace(nc, "Instant.now()");
                }
                if (CONSTRUCTOR_LONG.matches(newClass)) {
                    return replace(nc, "Instant.ofEpochMilli(#{any(long)})", nc.getArguments().get(0));
                }
                if (CONSTRUCTOR_OBJECT.matches(newClass)) {
                    Expression arg = nc.getArguments().get(0);
                    if (TypeUtils.isOfClassType(arg.getType(), "java.lang.Long")) {
                        return replace(nc, "Instant.ofEpochMilli(#{any(long)})", arg);
                    }
                    if (TypeUtils.isAssignableTo("java.util.Date", arg.getType())) {
                        return replace(nc, "Instant.ofEpochMilli(#{any(java.util.Date)}.getTime())", arg);
                    }
                    if (JodaTimeTypes.isDateTime(arg.getType())) {
                        return replace(nc, "#{any(java.time.ZonedDateTime)}.toInstant()", arg);
                    }
                    if (JodaTimeTypes.isInstant(arg.getType())) {
                        return replace(nc, "#{any(java.time.Instant)}", arg);
                    }
                }
                return nc;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (PLUS_LONG.matches(method)) {
                    return replace(m, "#{any(java.time.Instant)}.plusMillis(#{any(long)})", m.getSelect(), m.getArguments().get(0));
                }
                if (MINUS_LONG.matches(method)) {
                    return replace(m, "#{any(java.time.Instant)}.minusMillis(#{any(long)})", m.getSelect(), m.getArguments().get(0));
                }
                if (WITH_MILLIS.matches(method)) {
                    return replace(m, "Instant.ofEpochMilli(#{any(long)})", m.getArguments().get(0));
                }
                if (WITH_DURATION_ADDED_LONG.matches(method)) {
                    return replace(m, "#{any(java.time.Instant)}.plusMillis(#{any(long)} * #{any(int)})",
                            m.getSelect(), m.getArguments().get(0), m.getArguments().get(1));
                }
                if (WITH_DURATION_ADDED.matches(method)) {
                    return replace(m, "#{any(java.time.Instant)}.plus(#{any(java.time.Duration)}.multipliedBy(#{any(int)}))",
                            m.getSelect(), m.getArguments().get(0), m.getArguments().get(1));
                }
                return m;
            }

            private J replace(Expression expression, String code, Object... parameters) {
                maybeAddImport("java.time.Instant");
                return JavaTemplate.builder(code).imports("java.time.Instant").build()
                        .apply(getCursor(), expression.getCoordinates().replace(), parameters);
            }
        });
    }
}
