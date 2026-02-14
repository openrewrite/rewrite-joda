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
public class JodaDateTimeToJavaTime extends Recipe {
    String displayName = "Migrate Joda-Time DateTime to java.time.ZonedDateTime";

    String description = "Migrates Joda-Time `DateTime` constructors and instance methods to the equivalent `java.time.ZonedDateTime` calls.";

    // Constructor matchers
    private static final MethodMatcher NEW_DT = new MethodMatcher("org.joda.time.DateTime <constructor>()");
    private static final MethodMatcher NEW_DT_ZONE = new MethodMatcher("org.joda.time.DateTime <constructor>(org.joda.time.DateTimeZone)");
    private static final MethodMatcher NEW_DT_LONG = new MethodMatcher("org.joda.time.DateTime <constructor>(long)");
    private static final MethodMatcher NEW_DT_LONG_ZONE = new MethodMatcher("org.joda.time.DateTime <constructor>(long, org.joda.time.DateTimeZone)");
    private static final MethodMatcher NEW_DT_5 = new MethodMatcher("org.joda.time.DateTime <constructor>(int, int, int, int, int)");
    private static final MethodMatcher NEW_DT_5_ZONE = new MethodMatcher("org.joda.time.DateTime <constructor>(int, int, int, int, int, org.joda.time.DateTimeZone)");
    private static final MethodMatcher NEW_DT_6 = new MethodMatcher("org.joda.time.DateTime <constructor>(int, int, int, int, int, int)");
    private static final MethodMatcher NEW_DT_6_ZONE = new MethodMatcher("org.joda.time.DateTime <constructor>(int, int, int, int, int, int, org.joda.time.DateTimeZone)");
    private static final MethodMatcher NEW_DT_7 = new MethodMatcher("org.joda.time.DateTime <constructor>(int, int, int, int, int, int, int)");
    private static final MethodMatcher NEW_DT_7_ZONE = new MethodMatcher("org.joda.time.DateTime <constructor>(int, int, int, int, int, int, int, org.joda.time.DateTimeZone)");

    // Static factory matchers
    private static final MethodMatcher DT_NOW = new MethodMatcher("org.joda.time.DateTime now()");
    private static final MethodMatcher DT_NOW_ZONE = new MethodMatcher("org.joda.time.DateTime now(org.joda.time.DateTimeZone)");
    private static final MethodMatcher DT_PARSE = new MethodMatcher("org.joda.time.DateTime parse(String)");
    private static final MethodMatcher DT_PARSE_FMT = new MethodMatcher("org.joda.time.DateTime parse(String, org.joda.time.format.DateTimeFormatter)");

    // Instance method matchers (structural transformations only)
    private static final MethodMatcher TO_DATE_TIME = new MethodMatcher("org.joda.time.DateTime toDateTime()");
    private static final MethodMatcher TO_DATE_TIME_ZONE = new MethodMatcher("org.joda.time.DateTime toDateTime(org.joda.time.DateTimeZone)");
    private static final MethodMatcher TO_DATE_MIDNIGHT = new MethodMatcher("org.joda.time.DateTime toDateMidnight()");
    private static final MethodMatcher WITH_MILLIS = new MethodMatcher("org.joda.time.DateTime withMillis(long)");
    private static final MethodMatcher WITH_DATE = new MethodMatcher("org.joda.time.DateTime withDate(int, int, int)");
    private static final MethodMatcher WITH_DATE_LD = new MethodMatcher("org.joda.time.DateTime withDate(org.joda.time.LocalDate)");
    private static final MethodMatcher WITH_TIME = new MethodMatcher("org.joda.time.DateTime withTime(int, int, int, int)");
    private static final MethodMatcher WITH_TIME_LT = new MethodMatcher("org.joda.time.DateTime withTime(org.joda.time.LocalTime)");
    private static final MethodMatcher WITH_TIME_AT_START = new MethodMatcher("org.joda.time.DateTime withTimeAtStartOfDay()");
    private static final MethodMatcher WITH_DURATION_ADDED = new MethodMatcher("org.joda.time.DateTime withDurationAdded(long, int)");
    private static final MethodMatcher PLUS_LONG = new MethodMatcher("org.joda.time.DateTime plus(long)");
    private static final MethodMatcher PLUS_MILLIS = new MethodMatcher("org.joda.time.DateTime plusMillis(int)");
    private static final MethodMatcher MINUS_LONG = new MethodMatcher("org.joda.time.DateTime minus(long)");
    private static final MethodMatcher MINUS_MILLIS = new MethodMatcher("org.joda.time.DateTime minusMillis(int)");
    private static final MethodMatcher WITH_WEEKYEAR = new MethodMatcher("org.joda.time.DateTime withWeekyear(int)");
    private static final MethodMatcher WITH_WEEK_OF_WEEKYEAR = new MethodMatcher("org.joda.time.DateTime withWeekOfWeekyear(int)");
    private static final MethodMatcher WITH_DAY_OF_WEEK = new MethodMatcher("org.joda.time.DateTime withDayOfWeek(int)");
    private static final MethodMatcher WITH_MILLIS_OF_SECOND = new MethodMatcher("org.joda.time.DateTime withMillisOfSecond(int)");
    private static final MethodMatcher WITH_MILLIS_OF_DAY = new MethodMatcher("org.joda.time.DateTime withMillisOfDay(int)");

    // AbstractDateTime accessors that need structural changes
    private static final MethodMatcher GET_DAY_OF_WEEK = new MethodMatcher("org.joda.time.base.AbstractDateTime getDayOfWeek()");
    private static final MethodMatcher GET_MILLIS_OF_SECOND = new MethodMatcher("org.joda.time.base.AbstractDateTime getMillisOfSecond()");
    private static final MethodMatcher GET_MINUTE_OF_DAY = new MethodMatcher("org.joda.time.base.AbstractDateTime getMinuteOfDay()");
    private static final MethodMatcher GET_SECOND_OF_DAY = new MethodMatcher("org.joda.time.base.AbstractDateTime getSecondOfDay()");
    private static final MethodMatcher GET_WEEK_OF_WEEKYEAR = new MethodMatcher("org.joda.time.base.AbstractDateTime getWeekOfWeekyear()");
    private static final MethodMatcher GET_MILLIS_BASE = new MethodMatcher("org.joda.time.base.BaseDateTime getMillis()");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("org.joda.time.DateTime", true), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass nc = (J.NewClass) super.visitNewClass(newClass, ctx);
                if (NEW_DT.matches(newClass)) {
                    maybeAddImport("java.time.ZonedDateTime");
                    return JavaTemplate.builder("ZonedDateTime.now()")
                            .imports("java.time.ZonedDateTime").build()
                            .apply(getCursor(), nc.getCoordinates().replace());
                }
                if (NEW_DT_ZONE.matches(newClass)) {
                    maybeAddImport("java.time.ZonedDateTime");
                    return JavaTemplate.builder("ZonedDateTime.now(#{any(java.time.ZoneOffset)})")
                            .imports("java.time.ZonedDateTime").build()
                            .apply(getCursor(), nc.getCoordinates().replace(), nc.getArguments().get(0));
                }
                if (NEW_DT_LONG.matches(newClass)) {
                    maybeAddImport("java.time.ZonedDateTime");
                    maybeAddImport("java.time.Instant");
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("ZonedDateTime.ofInstant(Instant.ofEpochMilli(#{any(long)}), ZoneId.systemDefault())")
                            .imports("java.time.ZonedDateTime", "java.time.Instant", "java.time.ZoneId").build()
                            .apply(getCursor(), nc.getCoordinates().replace(), nc.getArguments().get(0));
                }
                if (NEW_DT_LONG_ZONE.matches(newClass)) {
                    maybeAddImport("java.time.ZonedDateTime");
                    maybeAddImport("java.time.Instant");
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("ZonedDateTime.ofInstant(Instant.ofEpochMilli(#{any(long)}), #{any(java.time.ZoneId)})")
                            .imports("java.time.ZonedDateTime", "java.time.Instant", "java.time.ZoneId").build()
                            .apply(getCursor(), nc.getCoordinates().replace(),
                            nc.getArguments().get(0), nc.getArguments().get(1));
                }
                if (NEW_DT_5.matches(newClass)) {
                    maybeAddImport("java.time.ZonedDateTime");
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("ZonedDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, 0, 0, ZoneId.systemDefault())")
                            .imports("java.time.ZonedDateTime", "java.time.ZoneId").build()
                            .apply(getCursor(), nc.getCoordinates().replace(),
                            nc.getArguments().get(0), nc.getArguments().get(1), nc.getArguments().get(2),
                            nc.getArguments().get(3), nc.getArguments().get(4));
                }
                if (NEW_DT_5_ZONE.matches(newClass)) {
                    maybeAddImport("java.time.ZonedDateTime");
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("ZonedDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, 0, 0, #{any(java.time.ZoneId)})")
                            .imports("java.time.ZonedDateTime", "java.time.ZoneId").build()
                            .apply(getCursor(), nc.getCoordinates().replace(),
                            nc.getArguments().get(0), nc.getArguments().get(1), nc.getArguments().get(2),
                            nc.getArguments().get(3), nc.getArguments().get(4), nc.getArguments().get(5));
                }
                if (NEW_DT_6.matches(newClass)) {
                    maybeAddImport("java.time.ZonedDateTime");
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("ZonedDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, 0, ZoneId.systemDefault())")
                            .imports("java.time.ZonedDateTime", "java.time.ZoneId").build()
                            .apply(getCursor(), nc.getCoordinates().replace(),
                            nc.getArguments().get(0), nc.getArguments().get(1), nc.getArguments().get(2),
                            nc.getArguments().get(3), nc.getArguments().get(4), nc.getArguments().get(5));
                }
                if (NEW_DT_6_ZONE.matches(newClass)) {
                    maybeAddImport("java.time.ZonedDateTime");
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("ZonedDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, 0, #{any(java.time.ZoneId)})")
                            .imports("java.time.ZonedDateTime", "java.time.ZoneId").build()
                            .apply(getCursor(), nc.getCoordinates().replace(),
                            nc.getArguments().get(0), nc.getArguments().get(1), nc.getArguments().get(2),
                            nc.getArguments().get(3), nc.getArguments().get(4), nc.getArguments().get(5),
                            nc.getArguments().get(6));
                }
                if (NEW_DT_7.matches(newClass)) {
                    maybeAddImport("java.time.ZonedDateTime");
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("ZonedDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)} * 1_000_000, ZoneId.systemDefault())")
                            .imports("java.time.ZonedDateTime", "java.time.ZoneId").build()
                            .apply(getCursor(), nc.getCoordinates().replace(),
                            nc.getArguments().get(0), nc.getArguments().get(1), nc.getArguments().get(2),
                            nc.getArguments().get(3), nc.getArguments().get(4), nc.getArguments().get(5),
                            nc.getArguments().get(6));
                }
                if (NEW_DT_7_ZONE.matches(newClass)) {
                    maybeAddImport("java.time.ZonedDateTime");
                    return JavaTemplate.builder("ZonedDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)} * 1_000_000, #{any(java.time.ZoneId)})")
                            .imports("java.time.ZonedDateTime").build()
                            .apply(getCursor(), nc.getCoordinates().replace(),
                            nc.getArguments().get(0), nc.getArguments().get(1), nc.getArguments().get(2),
                            nc.getArguments().get(3), nc.getArguments().get(4), nc.getArguments().get(5),
                            nc.getArguments().get(6), nc.getArguments().get(7));
                }
                return nc;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

                // Static factories
                if (DT_NOW.matches(method)) {
                    maybeAddImport("java.time.ZonedDateTime");
                    return JavaTemplate.builder("ZonedDateTime.now()")
                            .imports("java.time.ZonedDateTime").build()
                            .apply(getCursor(), m.getCoordinates().replace());
                }
                if (DT_NOW_ZONE.matches(method)) {
                    maybeAddImport("java.time.ZonedDateTime");
                    return JavaTemplate.builder("ZonedDateTime.now(#{any(java.time.ZoneOffset)})")
                            .imports("java.time.ZonedDateTime").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getArguments().get(0));
                }
                if (DT_PARSE.matches(method)) {
                    maybeAddImport("java.time.ZonedDateTime");
                    return JavaTemplate.builder("ZonedDateTime.parse(#{any(String)})")
                            .imports("java.time.ZonedDateTime").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getArguments().get(0));
                }
                if (DT_PARSE_FMT.matches(method)) {
                    maybeAddImport("java.time.ZonedDateTime");
                    return JavaTemplate.builder("ZonedDateTime.parse(#{any(String)}, #{any(java.time.format.DateTimeFormatter)})")
                            .imports("java.time.ZonedDateTime").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                            m.getArguments().get(0), m.getArguments().get(1));
                }

                // Identity removal
                if (TO_DATE_TIME.matches(method)) {
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (TO_DATE_TIME_ZONE.matches(method)) {
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withZoneSameInstant(#{any(java.time.ZoneId)})").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                            m.getSelect(), m.getArguments().get(0));
                }
                if (TO_DATE_MIDNIGHT.matches(method)) {
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.toLocalDate().atStartOfDay(ZoneId.systemDefault())")
                            .imports("java.time.ZoneId").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }

                // Arg reordering: withMillis(arg) -> ZonedDateTime.ofInstant(Instant.ofEpochMilli(arg), select.getZone())
                if (WITH_MILLIS.matches(method)) {
                    maybeAddImport("java.time.ZonedDateTime");
                    maybeAddImport("java.time.Instant");
                    return JavaTemplate.builder("ZonedDateTime.ofInstant(Instant.ofEpochMilli(#{any(long)}), #{any(java.time.ZonedDateTime)}.getZone())")
                            .imports("java.time.ZonedDateTime", "java.time.Instant").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                            m.getArguments().get(0), m.getSelect());
                }

                // Chain expansion
                if (WITH_DATE.matches(method)) {
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withYear(#{any(int)}).withMonth(#{any(int)}).withDayOfMonth(#{any(int)})").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                            m.getSelect(), m.getArguments().get(0), m.getArguments().get(1), m.getArguments().get(2));
                }
                if (WITH_DATE_LD.matches(method)) {
                    maybeAddImport("java.time.temporal.TemporalAdjuster");
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.with(#{any(java.time.temporal.TemporalAdjuster)})")
                            .imports("java.time.temporal.TemporalAdjuster").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                            m.getSelect(), m.getArguments().get(0));
                }
                if (WITH_TIME.matches(method)) {
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withHour(#{any(int)}).withMinute(#{any(int)}).withSecond(#{any(int)}).withNano(#{any(int)} * 1_000_000)").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                            m.getSelect(), m.getArguments().get(0), m.getArguments().get(1),
                            m.getArguments().get(2), m.getArguments().get(3));
                }
                if (WITH_TIME_LT.matches(method)) {
                    maybeAddImport("java.time.temporal.TemporalAdjuster");
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.with(#{any(java.time.temporal.TemporalAdjuster)})")
                            .imports("java.time.temporal.TemporalAdjuster").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                            m.getSelect(), m.getArguments().get(0));
                }
                if (WITH_TIME_AT_START.matches(method)) {
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.toLocalDate().atStartOfDay(#{any(java.time.ZonedDateTime)}.getZone())").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                            m.getSelect(), m.getSelect());
                }

                // Duration-related
                if (WITH_DURATION_ADDED.matches(method)) {
                    maybeAddImport("java.time.Duration");
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plus(Duration.ofMillis(#{any(long)}).multipliedBy(#{any(int)}))")
                            .imports("java.time.Duration").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                            m.getSelect(), m.getArguments().get(0), m.getArguments().get(1));
                }
                if (PLUS_LONG.matches(method) || PLUS_MILLIS.matches(method)) {
                    maybeAddImport("java.time.Duration");
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plus(Duration.ofMillis(#{any(int)}))")
                            .imports("java.time.Duration").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                            m.getSelect(), m.getArguments().get(0));
                }
                if (MINUS_LONG.matches(method) || MINUS_MILLIS.matches(method)) {
                    maybeAddImport("java.time.Duration");
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.minus(Duration.ofMillis(#{any(int)}))")
                            .imports("java.time.Duration").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                            m.getSelect(), m.getArguments().get(0));
                }

                // ChronoField/IsoFields setters
                if (WITH_WEEKYEAR.matches(method)) {
                    maybeAddImport("java.time.temporal.IsoFields");
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.with(IsoFields.WEEK_BASED_YEAR, #{any(int)})")
                            .imports("java.time.temporal.IsoFields").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                            m.getSelect(), m.getArguments().get(0));
                }
                if (WITH_WEEK_OF_WEEKYEAR.matches(method)) {
                    maybeAddImport("java.time.temporal.ChronoField");
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.with(ChronoField.ALIGNED_WEEK_OF_YEAR, #{any(int)})")
                            .imports("java.time.temporal.ChronoField").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                            m.getSelect(), m.getArguments().get(0));
                }
                if (WITH_DAY_OF_WEEK.matches(method)) {
                    maybeAddImport("java.time.temporal.ChronoField");
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.with(ChronoField.DAY_OF_WEEK, #{any(int)})")
                            .imports("java.time.temporal.ChronoField").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                            m.getSelect(), m.getArguments().get(0));
                }
                if (WITH_MILLIS_OF_SECOND.matches(method)) {
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withNano(#{any(int)} * 1_000_000)").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                            m.getSelect(), m.getArguments().get(0));
                }
                if (WITH_MILLIS_OF_DAY.matches(method)) {
                    maybeAddImport("java.time.temporal.ChronoField");
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.with(ChronoField.MILLI_OF_DAY, #{any(int)})")
                            .imports("java.time.temporal.ChronoField").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                            m.getSelect(), m.getArguments().get(0));
                }

                // AbstractDateTime getters that need structural changes
                if (GET_DAY_OF_WEEK.matches(method)) {
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.getDayOfWeek().getValue()").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (GET_MILLIS_OF_SECOND.matches(method)) {
                    maybeAddImport("java.time.temporal.ChronoField");
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.get(ChronoField.MILLI_OF_SECOND)")
                            .imports("java.time.temporal.ChronoField").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (GET_MINUTE_OF_DAY.matches(method)) {
                    maybeAddImport("java.time.temporal.ChronoField");
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.get(ChronoField.MINUTE_OF_DAY)")
                            .imports("java.time.temporal.ChronoField").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (GET_SECOND_OF_DAY.matches(method)) {
                    maybeAddImport("java.time.temporal.ChronoField");
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.get(ChronoField.SECOND_OF_DAY)")
                            .imports("java.time.temporal.ChronoField").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (GET_WEEK_OF_WEEKYEAR.matches(method)) {
                    maybeAddImport("java.time.temporal.ChronoField");
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.get(ChronoField.ALIGNED_WEEK_OF_YEAR)")
                            .imports("java.time.temporal.ChronoField").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (GET_MILLIS_BASE.matches(method)) {
                    return JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.toInstant().toEpochMilli()").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                return m;
            }
        });
    }
}
