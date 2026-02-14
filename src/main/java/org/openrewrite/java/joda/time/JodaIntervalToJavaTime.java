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
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class JodaIntervalToJavaTime extends Recipe {
    String displayName = "Migrate Joda-Time Interval to Java time";

    String description = "Migrates `org.joda.time.Interval` constructors and methods to their Java time equivalents using ThreeTen-Extra.";

    // Constructors
    private static final MethodMatcher NEW_INTERVAL_LONGS = new MethodMatcher("org.joda.time.Interval <constructor>(long, long)");
    private static final MethodMatcher NEW_INTERVAL_LONGS_ZONE = new MethodMatcher("org.joda.time.Interval <constructor>(long, long, org.joda.time.DateTimeZone)");
    private static final MethodMatcher NEW_INTERVAL_RI_RI = new MethodMatcher("org.joda.time.Interval <constructor>(org.joda.time.ReadableInstant, org.joda.time.ReadableInstant)");
    private static final MethodMatcher NEW_INTERVAL_RI_RD = new MethodMatcher("org.joda.time.Interval <constructor>(org.joda.time.ReadableInstant, org.joda.time.ReadableDuration)");
    // AbstractInterval methods
    private static final MethodMatcher GET_START = new MethodMatcher("org.joda.time.base.AbstractInterval getStart()");
    private static final MethodMatcher GET_END = new MethodMatcher("org.joda.time.base.AbstractInterval getEnd()");
    private static final MethodMatcher TO_DURATION_MILLIS = new MethodMatcher("org.joda.time.base.AbstractInterval toDurationMillis()");
    private static final MethodMatcher CONTAINS = new MethodMatcher("org.joda.time.base.AbstractInterval contains(long)");
    // BaseInterval methods
    private static final MethodMatcher GET_START_MILLIS = new MethodMatcher("org.joda.time.base.BaseInterval getStartMillis()");
    private static final MethodMatcher GET_END_MILLIS = new MethodMatcher("org.joda.time.base.BaseInterval getEndMillis()");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("org.joda.time.*Interval*", true), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass nc = (J.NewClass) super.visitNewClass(newClass, ctx);
                if (NEW_INTERVAL_LONGS.matches(newClass) || NEW_INTERVAL_LONGS_ZONE.matches(newClass)) {
                    maybeAddImport("java.time.Instant");
                    maybeAddImport("org.threeten.extra.Interval");
                    return JavaTemplate.builder("Interval.of(Instant.ofEpochMilli(#{any(long)}), Instant.ofEpochMilli(#{any(long)}))")
                            .javaParser(JavaParser.fromJavaVersion().classpath("threeten-extra"))
                            .imports("java.time.Instant", "org.threeten.extra.Interval").build()
                            .apply(getCursor(), nc.getCoordinates().replace(),
                                    nc.getArguments().get(0), nc.getArguments().get(1));
                }
                if (NEW_INTERVAL_RI_RI.matches(newClass)) {
                    maybeAddImport("org.threeten.extra.Interval");
                    return JavaTemplate.builder("Interval.of(#{any(java.time.ZonedDateTime)}.toInstant(), #{any(java.time.ZonedDateTime)}.toInstant())")
                            .javaParser(JavaParser.fromJavaVersion().classpath("threeten-extra"))
                            .imports("org.threeten.extra.Interval").build()
                            .apply(getCursor(), nc.getCoordinates().replace(),
                                    nc.getArguments().get(0), nc.getArguments().get(1));
                }
                if (NEW_INTERVAL_RI_RD.matches(newClass)) {
                    maybeAddImport("org.threeten.extra.Interval");
                    return JavaTemplate.builder("Interval.of(#{any(java.time.ZonedDateTime)}.toInstant(), #{any(java.time.Duration)})")
                            .javaParser(JavaParser.fromJavaVersion().classpath("threeten-extra"))
                            .imports("org.threeten.extra.Interval").build()
                            .apply(getCursor(), nc.getCoordinates().replace(),
                                    nc.getArguments().get(0), nc.getArguments().get(1));
                }
                return nc;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (GET_START.matches(method)) {
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("#{any(org.threeten.extra.Interval)}.getStart().atZone(ZoneId.systemDefault())")
                            .javaParser(JavaParser.fromJavaVersion().classpath("threeten-extra"))
                            .imports("java.time.ZoneId").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (GET_END.matches(method)) {
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("#{any(org.threeten.extra.Interval)}.getEnd().atZone(ZoneId.systemDefault())")
                            .javaParser(JavaParser.fromJavaVersion().classpath("threeten-extra"))
                            .imports("java.time.ZoneId").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (TO_DURATION_MILLIS.matches(method)) {
                    return JavaTemplate.builder("#{any(org.threeten.extra.Interval)}.toDuration().toMillis()")
                            .javaParser(JavaParser.fromJavaVersion().classpath("threeten-extra")).build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (CONTAINS.matches(method)) {
                    maybeAddImport("java.time.Instant");
                    return JavaTemplate.builder("#{any(org.threeten.extra.Interval)}.contains(Instant.ofEpochMilli(#{any(long)}))")
                            .javaParser(JavaParser.fromJavaVersion().classpath("threeten-extra"))
                            .imports("java.time.Instant").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                                    m.getSelect(), m.getArguments().get(0));
                }
                if (GET_START_MILLIS.matches(method)) {
                    return JavaTemplate.builder("#{any(org.threeten.extra.Interval)}.getStart().toEpochMilli()")
                            .javaParser(JavaParser.fromJavaVersion().classpath("threeten-extra")).build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (GET_END_MILLIS.matches(method)) {
                    return JavaTemplate.builder("#{any(org.threeten.extra.Interval)}.getEnd().toEpochMilli()")
                            .javaParser(JavaParser.fromJavaVersion().classpath("threeten-extra")).build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                return m;
            }
        });
    }
}
