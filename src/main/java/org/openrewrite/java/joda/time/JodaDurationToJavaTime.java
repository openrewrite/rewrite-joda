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
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class JodaDurationToJavaTime extends Recipe {
    String displayName = "Migrate Joda-Time Duration to Java time";

    String description = "Migrates `org.joda.time.Duration` constructor and method calls to `java.time.Duration`.";

    private static final MethodMatcher NEW_DURATION = new MethodMatcher("org.joda.time.Duration <constructor>(long)");
    private static final MethodMatcher NEW_DURATION_BETWEEN = new MethodMatcher("org.joda.time.Duration <constructor>(long, long)");
    private static final MethodMatcher TO_DURATION = new MethodMatcher("org.joda.time.Duration toDuration()");
    private static final MethodMatcher WITH_MILLIS = new MethodMatcher("org.joda.time.Duration withMillis(long)");
    private static final MethodMatcher WITH_DURATION_ADDED_LONG = new MethodMatcher("org.joda.time.Duration withDurationAdded(long, int)");
    private static final MethodMatcher WITH_DURATION_ADDED_RD = new MethodMatcher("org.joda.time.Duration withDurationAdded(org.joda.time.ReadableDuration, int)");
    private static final MethodMatcher PLUS_LONG = new MethodMatcher("org.joda.time.Duration plus(long)");
    private static final MethodMatcher MINUS_LONG = new MethodMatcher("org.joda.time.Duration minus(long)");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("org.joda.time.Duration", true), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass nc = (J.NewClass) super.visitNewClass(newClass, ctx);
                if (NEW_DURATION.matches(newClass)) {
                    maybeAddImport("java.time.Duration");
                    return JavaTemplate.builder("Duration.ofMillis(#{any(long)})")
                            .imports("java.time.Duration").build()
                            .apply(getCursor(), nc.getCoordinates().replace(), nc.getArguments().get(0));
                }
                if (NEW_DURATION_BETWEEN.matches(newClass)) {
                    maybeAddImport("java.time.Duration");
                    maybeAddImport("java.time.Instant");
                    return JavaTemplate.builder("Duration.between(Instant.ofEpochMilli(#{any(long)}), Instant.ofEpochMilli(#{any(long)}))")
                            .imports("java.time.Duration", "java.time.Instant").build()
                            .apply(getCursor(), nc.getCoordinates().replace(),
                                    nc.getArguments().get(0), nc.getArguments().get(1));
                }
                return nc;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (TO_DURATION.matches(method)) {
                    return JavaTemplate.builder("#{any(java.time.Duration)}").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (WITH_MILLIS.matches(method)) {
                    maybeAddImport("java.time.Duration");
                    return JavaTemplate.builder("Duration.ofMillis(#{any(long)})")
                            .imports("java.time.Duration").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getArguments().get(0));
                }
                if (WITH_DURATION_ADDED_LONG.matches(method)) {
                    return JavaTemplate.builder("#{any(java.time.Duration)}.plusMillis(#{any(long)} * #{any(int)})").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                                    m.getSelect(), m.getArguments().get(0), m.getArguments().get(1));
                }
                if (WITH_DURATION_ADDED_RD.matches(method)) {
                    return JavaTemplate.builder("#{any(java.time.Duration)}.plus(#{any(java.time.Duration)}.multipliedBy(#{any(int)}))").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                                    m.getSelect(), m.getArguments().get(0), m.getArguments().get(1));
                }
                if (PLUS_LONG.matches(method)) {
                    return JavaTemplate.builder("#{any(java.time.Duration)}.plusMillis(#{any(long)})").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                                    m.getSelect(), m.getArguments().get(0));
                }
                if (MINUS_LONG.matches(method)) {
                    return JavaTemplate.builder("#{any(java.time.Duration)}.minusMillis(#{any(long)})").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                                    m.getSelect(), m.getArguments().get(0));
                }
                return m;
            }
        });
    }
}
