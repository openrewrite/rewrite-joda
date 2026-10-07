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
public class JodaLocalDateToJavaTime extends Recipe {
    String displayName = "Migrate Joda-Time `LocalDate` to `java.time.LocalDate`";

    String description = "Migrates Joda-Time `LocalDate` constructors and instance methods to the equivalent `java.time.LocalDate` calls.";

    // Constructor matchers
    private static final MethodMatcher NEW_LD = new MethodMatcher("org.joda.time.LocalDate <constructor>()");
    private static final MethodMatcher NEW_LD_ZONE = new MethodMatcher("org.joda.time.LocalDate <constructor>(org.joda.time.DateTimeZone)");
    private static final MethodMatcher NEW_LD_YMD = new MethodMatcher("org.joda.time.LocalDate <constructor>(int, int, int)");
    private static final MethodMatcher NEW_LD_MILLIS = new MethodMatcher("org.joda.time.LocalDate <constructor>(long)");
    private static final MethodMatcher NEW_LD_MILLIS_ZONE = new MethodMatcher("org.joda.time.LocalDate <constructor>(long, org.joda.time.DateTimeZone)");

    private static final MethodMatcher NEW_LD_OBJECT = new MethodMatcher("org.joda.time.LocalDate <constructor>(Object)");
    private static final MethodMatcher NEW_LD_OBJECT_ZONE = new MethodMatcher("org.joda.time.LocalDate <constructor>(Object, org.joda.time.DateTimeZone)");
    private static final MethodMatcher FROM_DATE_FIELDS = new MethodMatcher("org.joda.time.LocalDate fromDateFields(java.util.Date)");
    private static final MethodMatcher LDT_FROM_DATE_FIELDS = new MethodMatcher("org.joda.time.LocalDateTime fromDateFields(java.util.Date)");

    // Instance method matchers
    private static final MethodMatcher TO_DATE = new MethodMatcher("org.joda.time.LocalDate toDate()");
    private static final MethodMatcher LDT_TO_DATE = new MethodMatcher("org.joda.time.LocalDateTime toDate()");
    private static final MethodMatcher TO_DATE_MIDNIGHT = new MethodMatcher("org.joda.time.LocalDate toDateMidnight()");
    private static final MethodMatcher GET_DAY_OF_WEEK = new MethodMatcher("org.joda.time.LocalDate getDayOfWeek()");
    private static final MethodMatcher TO_DT_AT_START = new MethodMatcher("org.joda.time.LocalDate toDateTimeAtStartOfDay()");
    private static final MethodMatcher TO_DT_AT_START_ZONE = new MethodMatcher("org.joda.time.LocalDate toDateTimeAtStartOfDay(org.joda.time.DateTimeZone)");
    private static final MethodMatcher TO_LOCAL_DATE_TIME = new MethodMatcher("org.joda.time.LocalDate toLocalDateTime(org.joda.time.LocalTime)");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("org.joda.time.Local*", true), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass nc = (J.NewClass) super.visitNewClass(newClass, ctx);
                if (NEW_LD.matches(newClass)) {
                    maybeAddImport("java.time.LocalDate");
                    return JavaTemplate.builder("LocalDate.now()")
                            .imports("java.time.LocalDate").build()
                            .apply(getCursor(), nc.getCoordinates().replace());
                }
                if (NEW_LD_ZONE.matches(newClass)) {
                    maybeAddImport("java.time.LocalDate");
                    return JavaTemplate.builder("LocalDate.now(#{any(java.time.ZoneId)})")
                            .imports("java.time.LocalDate").build()
                            .apply(getCursor(), nc.getCoordinates().replace(), nc.getArguments().get(0));
                }
                if (NEW_LD_YMD.matches(newClass)) {
                    maybeAddImport("java.time.LocalDate");
                    return JavaTemplate.builder("LocalDate.of(#{any(int)}, #{any(int)}, #{any(int)})")
                            .imports("java.time.LocalDate").build()
                            .apply(getCursor(), nc.getCoordinates().replace(),
                                    nc.getArguments().get(0), nc.getArguments().get(1), nc.getArguments().get(2));
                }
                if (NEW_LD_MILLIS.matches(newClass)) {
                    maybeAddImport("java.time.Instant");
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("Instant.ofEpochMilli(#{any(long)}).atZone(ZoneId.systemDefault()).toLocalDate()")
                            .imports("java.time.Instant", "java.time.ZoneId").build()
                            .apply(getCursor(), nc.getCoordinates().replace(), nc.getArguments().get(0));
                }
                if (NEW_LD_MILLIS_ZONE.matches(newClass)) {
                    maybeAddImport("java.time.Instant");
                    return JavaTemplate.builder("Instant.ofEpochMilli(#{any(long)}).atZone(#{any(java.time.ZoneId)}).toLocalDate()")
                            .imports("java.time.Instant").build()
                            .apply(getCursor(), nc.getCoordinates().replace(),
                                    nc.getArguments().get(0), nc.getArguments().get(1));
                }
                if (NEW_LD_OBJECT.matches(newClass) || NEW_LD_OBJECT_ZONE.matches(newClass)) {
                    Expression instant = nc.getArguments().get(0);
                    boolean withZone = nc.getArguments().size() == 2;
                    String zone = withZone ? "#{any(java.time.ZoneId)}" : "ZoneId.systemDefault()";
                    String code = null;
                    if (TypeUtils.isAssignableTo("java.util.Date", instant.getType())) {
                        code = "Instant.ofEpochMilli(#{any(java.util.Date)}.getTime()).atZone(" + zone + ").toLocalDate()";
                    } else if (TypeUtils.isOfClassType(instant.getType(), "java.lang.Long")) {
                        code = "Instant.ofEpochMilli(#{any(long)}).atZone(" + zone + ").toLocalDate()";
                    } else if (JodaTimeTypes.isDateTime(instant.getType())) {
                        code = withZone ? "#{any(java.time.ZonedDateTime)}.withZoneSameInstant(" + zone + ").toLocalDate()" : "#{any(java.time.ZonedDateTime)}.toLocalDate()";
                    } else if (!withZone && JodaTimeTypes.isLocalDate(instant.getType())) {
                        code = "#{any(java.time.LocalDate)}";
                    }
                    if (code != null) {
                        maybeAddImport("java.time.Instant");
                        maybeAddImport("java.time.ZoneId");
                        JavaTemplate template = JavaTemplate.builder(code).imports("java.time.Instant", "java.time.ZoneId").build();
                        return withZone ?
                                template.apply(getCursor(), nc.getCoordinates().replace(), instant, nc.getArguments().get(1)) :
                                template.apply(getCursor(), nc.getCoordinates().replace(), instant);
                    }
                }
                return nc;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (FROM_DATE_FIELDS.matches(method) || LDT_FROM_DATE_FIELDS.matches(method)) {
                    maybeAddImport("java.time.Instant");
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("Instant.ofEpochMilli(#{any(java.util.Date)}.getTime()).atZone(ZoneId.systemDefault())." +
                                    (FROM_DATE_FIELDS.matches(method) ? "toLocalDate()" : "toLocalDateTime()"))
                            .imports("java.time.Instant", "java.time.ZoneId").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getArguments().get(0));
                }
                if (TO_DATE.matches(method) || LDT_TO_DATE.matches(method)) {
                    maybeAddImport("java.time.ZoneId");
                    maybeAddImport("java.util.Date");
                    return JavaTemplate.builder(TO_DATE.matches(method) ?
                                    "Date.from(#{any(java.time.LocalDate)}.atStartOfDay(ZoneId.systemDefault()).toInstant())" :
                                    "Date.from(#{any(java.time.LocalDateTime)}.atZone(ZoneId.systemDefault()).toInstant())")
                            .imports("java.time.ZoneId", "java.util.Date").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (TO_DATE_MIDNIGHT.matches(method)) {
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("#{any(java.time.LocalDate)}.atStartOfDay(ZoneId.systemDefault())")
                            .imports("java.time.ZoneId").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (GET_DAY_OF_WEEK.matches(method)) {
                    return JavaTemplate.apply("#{any(java.time.LocalDate)}.getDayOfWeek().getValue()", getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (TO_DT_AT_START.matches(method)) {
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("#{any(java.time.LocalDate)}.atStartOfDay(ZoneId.systemDefault())")
                            .imports("java.time.ZoneId").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (TO_DT_AT_START_ZONE.matches(method)) {
                    return JavaTemplate.apply("#{any(java.time.LocalDate)}.atStartOfDay(#{any(java.time.ZoneId)})", getCursor(), m.getCoordinates().replace(), m.getSelect(), m.getArguments().get(0));
                }
                if (TO_LOCAL_DATE_TIME.matches(method)) {
                    return JavaTemplate.apply("#{any(java.time.LocalDate)}.atTime(#{any(java.time.LocalTime)})", getCursor(), m.getCoordinates().replace(), m.getSelect(), m.getArguments().get(0));
                }
                return m;
            }
        });
    }
}
