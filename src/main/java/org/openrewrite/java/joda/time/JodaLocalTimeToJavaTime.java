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
public class JodaLocalTimeToJavaTime extends Recipe {
    String displayName = "Migrate Joda-Time LocalTime to java.time.LocalTime";

    String description = "Migrates Joda-Time `LocalTime` constructors and instance methods to the equivalent `java.time.LocalTime` calls.";

    // Constructor matchers
    private static final MethodMatcher NEW_LT = new MethodMatcher("org.joda.time.LocalTime <constructor>()");
    private static final MethodMatcher NEW_LT_ZONE = new MethodMatcher("org.joda.time.LocalTime <constructor>(org.joda.time.DateTimeZone)");
    private static final MethodMatcher NEW_LT_HM = new MethodMatcher("org.joda.time.LocalTime <constructor>(int, int)");
    private static final MethodMatcher NEW_LT_HMS = new MethodMatcher("org.joda.time.LocalTime <constructor>(int, int, int)");
    private static final MethodMatcher NEW_LT_HMSM = new MethodMatcher("org.joda.time.LocalTime <constructor>(int, int, int, int)");

    // Instance method matchers
    private static final MethodMatcher PLUS_MILLIS = new MethodMatcher("org.joda.time.LocalTime plusMillis(int)");
    private static final MethodMatcher MINUS_MILLIS = new MethodMatcher("org.joda.time.LocalTime minusMillis(int)");
    private static final MethodMatcher WITH_MILLIS_OF_SECOND = new MethodMatcher("org.joda.time.LocalTime withMillisOfSecond(int)");
    private static final MethodMatcher GET_MILLIS_OF_SECOND = new MethodMatcher("org.joda.time.LocalTime getMillisOfSecond()");
    private static final MethodMatcher GET_MILLIS_OF_DAY = new MethodMatcher("org.joda.time.LocalTime getMillisOfDay()");
    private static final MethodMatcher TO_DT_TODAY = new MethodMatcher("org.joda.time.LocalTime toDateTimeToday()");
    private static final MethodMatcher TO_DT_TODAY_ZONE = new MethodMatcher("org.joda.time.LocalTime toDateTimeToday(org.joda.time.DateTimeZone)");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("org.joda.time.LocalTime", true), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass nc = (J.NewClass) super.visitNewClass(newClass, ctx);
                if (NEW_LT.matches(newClass)) {
                    maybeAddImport("java.time.LocalTime");
                    return JavaTemplate.builder("LocalTime.now()")
                            .imports("java.time.LocalTime").build()
                            .apply(getCursor(), nc.getCoordinates().replace());
                }
                if (NEW_LT_ZONE.matches(newClass)) {
                    maybeAddImport("java.time.LocalTime");
                    return JavaTemplate.builder("LocalTime.now(#{any(java.time.ZoneId)})")
                            .imports("java.time.LocalTime").build()
                            .apply(getCursor(), nc.getCoordinates().replace(), nc.getArguments().get(0));
                }
                if (NEW_LT_HM.matches(newClass)) {
                    maybeAddImport("java.time.LocalTime");
                    return JavaTemplate.builder("LocalTime.of(#{any(int)}, #{any(int)})")
                            .imports("java.time.LocalTime").build()
                            .apply(getCursor(), nc.getCoordinates().replace(),
                                    nc.getArguments().get(0), nc.getArguments().get(1));
                }
                if (NEW_LT_HMS.matches(newClass)) {
                    maybeAddImport("java.time.LocalTime");
                    return JavaTemplate.builder("LocalTime.of(#{any(int)}, #{any(int)}, #{any(int)})")
                            .imports("java.time.LocalTime").build()
                            .apply(getCursor(), nc.getCoordinates().replace(),
                                    nc.getArguments().get(0), nc.getArguments().get(1), nc.getArguments().get(2));
                }
                if (NEW_LT_HMSM.matches(newClass)) {
                    maybeAddImport("java.time.LocalTime");
                    return JavaTemplate.builder("LocalTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)} * 1_000_000)")
                            .imports("java.time.LocalTime").build()
                            .apply(getCursor(), nc.getCoordinates().replace(),
                                    nc.getArguments().get(0), nc.getArguments().get(1),
                                    nc.getArguments().get(2), nc.getArguments().get(3));
                }
                return nc;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (PLUS_MILLIS.matches(method)) {
                    return JavaTemplate.builder("#{any(java.time.LocalTime)}.plusNanos(#{any(int)} * 1_000_000L)").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                                    m.getSelect(), m.getArguments().get(0));
                }
                if (MINUS_MILLIS.matches(method)) {
                    return JavaTemplate.builder("#{any(java.time.LocalTime)}.minusNanos(#{any(int)} * 1_000_000L)").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                                    m.getSelect(), m.getArguments().get(0));
                }
                if (WITH_MILLIS_OF_SECOND.matches(method)) {
                    return JavaTemplate.builder("#{any(java.time.LocalTime)}.withNano(#{any(int)} * 1_000_000)").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                                    m.getSelect(), m.getArguments().get(0));
                }
                if (GET_MILLIS_OF_SECOND.matches(method)) {
                    maybeAddImport("java.time.temporal.ChronoField");
                    return JavaTemplate.builder("#{any(java.time.LocalTime)}.get(ChronoField.MILLI_OF_SECOND)")
                            .imports("java.time.temporal.ChronoField").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (GET_MILLIS_OF_DAY.matches(method)) {
                    maybeAddImport("java.time.temporal.ChronoField");
                    return JavaTemplate.builder("#{any(java.time.LocalTime)}.get(ChronoField.MILLI_OF_DAY)")
                            .imports("java.time.temporal.ChronoField").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (TO_DT_TODAY.matches(method)) {
                    maybeAddImport("java.time.LocalDate");
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("#{any(java.time.LocalTime)}.atDate(LocalDate.now()).atZone(ZoneId.systemDefault())")
                            .imports("java.time.LocalDate", "java.time.ZoneId").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (TO_DT_TODAY_ZONE.matches(method)) {
                    maybeAddImport("java.time.LocalDate");
                    return JavaTemplate.builder("#{any(java.time.LocalTime)}.atDate(LocalDate.now(#{any(java.time.ZoneId)})).atZone(#{any(java.time.ZoneId)})")
                            .imports("java.time.LocalDate").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                                    m.getSelect(), m.getArguments().get(0), m.getArguments().get(0));
                }
                return m;
            }
        });
    }
}
