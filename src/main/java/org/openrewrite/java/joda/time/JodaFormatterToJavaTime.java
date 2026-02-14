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
import lombok.Getter;
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
public class JodaFormatterToJavaTime extends Recipe {
    @Getter
    String displayName = "Migrate Joda-Time formatter to Java time";

    @Getter
    String description = "Migrates Joda-Time `DateTimeFormatter` and `DateTimeFormat` method calls to their Java time equivalents.";

    // DateTimeFormatter methods (arg reordering)
    private static final MethodMatcher PARSE_DATE_TIME = new MethodMatcher("org.joda.time.format.DateTimeFormatter parseDateTime(java.lang.String)");
    private static final MethodMatcher PARSE_MILLIS = new MethodMatcher("org.joda.time.format.DateTimeFormatter parseMillis(java.lang.String)");
    private static final MethodMatcher PRINT_LONG = new MethodMatcher("org.joda.time.format.DateTimeFormatter print(long)");
    private static final MethodMatcher PRINT_READABLE_INSTANT = new MethodMatcher("org.joda.time.format.DateTimeFormatter print(org.joda.time.ReadableInstant)");
    private static final MethodMatcher WITH_ZONE_UTC = new MethodMatcher("org.joda.time.format.DateTimeFormatter withZoneUTC()");

    // DateTimeFormat localized methods
    private static final MethodMatcher SHORT_DATE = new MethodMatcher("org.joda.time.format.DateTimeFormat shortDate()");
    private static final MethodMatcher MEDIUM_DATE = new MethodMatcher("org.joda.time.format.DateTimeFormat mediumDate()");
    private static final MethodMatcher LONG_DATE = new MethodMatcher("org.joda.time.format.DateTimeFormat longDate()");
    private static final MethodMatcher FULL_DATE = new MethodMatcher("org.joda.time.format.DateTimeFormat fullDate()");
    private static final MethodMatcher SHORT_TIME = new MethodMatcher("org.joda.time.format.DateTimeFormat shortTime()");
    private static final MethodMatcher MEDIUM_TIME = new MethodMatcher("org.joda.time.format.DateTimeFormat mediumTime()");
    private static final MethodMatcher LONG_TIME = new MethodMatcher("org.joda.time.format.DateTimeFormat longTime()");
    private static final MethodMatcher FULL_TIME = new MethodMatcher("org.joda.time.format.DateTimeFormat fullTime()");
    private static final MethodMatcher SHORT_DATE_TIME = new MethodMatcher("org.joda.time.format.DateTimeFormat shortDateTime()");
    private static final MethodMatcher MEDIUM_DATE_TIME = new MethodMatcher("org.joda.time.format.DateTimeFormat mediumDateTime()");
    private static final MethodMatcher LONG_DATE_TIME = new MethodMatcher("org.joda.time.format.DateTimeFormat longDateTime()");
    private static final MethodMatcher FULL_DATE_TIME = new MethodMatcher("org.joda.time.format.DateTimeFormat fullDateTime()");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        // Templates for DateTimeFormatter methods
        JavaTemplate PARSE_DATE_TIME_T = JavaTemplate
                .builder("ZonedDateTime.parse(#{any(java.lang.String)}, #{any(java.time.format.DateTimeFormatter)})")
                .imports("java.time.ZonedDateTime").build();
        JavaTemplate PARSE_MILLIS_T = JavaTemplate
                .builder("ZonedDateTime.parse(#{any(java.lang.String)}, #{any(java.time.format.DateTimeFormatter)}).toInstant().toEpochMilli()")
                .imports("java.time.ZonedDateTime").build();
        JavaTemplate PRINT_LONG_T = JavaTemplate
                .builder("ZonedDateTime.ofInstant(Instant.ofEpochMilli(#{any(long)}), ZoneId.systemDefault()).format(#{any(java.time.format.DateTimeFormatter)})")
                .imports("java.time.ZonedDateTime", "java.time.Instant", "java.time.ZoneId").build();
        JavaTemplate PRINT_READABLE_INSTANT_T = JavaTemplate
                .builder("#{any(java.time.ZonedDateTime)}.format(#{any(java.time.format.DateTimeFormatter)})").build();
        JavaTemplate WITH_ZONE_UTC_T = JavaTemplate
                .builder("#{any(java.time.format.DateTimeFormatter)}.withZone(ZoneOffset.UTC)")
                .imports("java.time.ZoneOffset").build();

        // Templates for DateTimeFormat localized methods
        JavaTemplate SHORT_DATE_T = JavaTemplate
                .builder("DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)")
                .imports("java.time.format.DateTimeFormatter", "java.time.format.FormatStyle").build();
        JavaTemplate MEDIUM_DATE_T = JavaTemplate
                .builder("DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)")
                .imports("java.time.format.DateTimeFormatter", "java.time.format.FormatStyle").build();
        JavaTemplate LONG_DATE_T = JavaTemplate
                .builder("DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)")
                .imports("java.time.format.DateTimeFormatter", "java.time.format.FormatStyle").build();
        JavaTemplate FULL_DATE_T = JavaTemplate
                .builder("DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)")
                .imports("java.time.format.DateTimeFormatter", "java.time.format.FormatStyle").build();
        JavaTemplate SHORT_TIME_T = JavaTemplate
                .builder("DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)")
                .imports("java.time.format.DateTimeFormatter", "java.time.format.FormatStyle").build();
        JavaTemplate MEDIUM_TIME_T = JavaTemplate
                .builder("DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)")
                .imports("java.time.format.DateTimeFormatter", "java.time.format.FormatStyle").build();
        JavaTemplate LONG_TIME_T = JavaTemplate
                .builder("DateTimeFormatter.ofLocalizedTime(FormatStyle.LONG)")
                .imports("java.time.format.DateTimeFormatter", "java.time.format.FormatStyle").build();
        JavaTemplate FULL_TIME_T = JavaTemplate
                .builder("DateTimeFormatter.ofLocalizedTime(FormatStyle.FULL)")
                .imports("java.time.format.DateTimeFormatter", "java.time.format.FormatStyle").build();
        JavaTemplate SHORT_DATE_TIME_T = JavaTemplate
                .builder("DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)")
                .imports("java.time.format.DateTimeFormatter", "java.time.format.FormatStyle").build();
        JavaTemplate MEDIUM_DATE_TIME_T = JavaTemplate
                .builder("DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM)")
                .imports("java.time.format.DateTimeFormatter", "java.time.format.FormatStyle").build();
        JavaTemplate LONG_DATE_TIME_T = JavaTemplate
                .builder("DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.LONG)")
                .imports("java.time.format.DateTimeFormatter", "java.time.format.FormatStyle").build();
        JavaTemplate FULL_DATE_TIME_T = JavaTemplate
                .builder("DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.FULL)")
                .imports("java.time.format.DateTimeFormatter", "java.time.format.FormatStyle").build();

        JavaVisitor<ExecutionContext> visitor = new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

                // DateTimeFormatter methods (arg reordering: formatter.method(arg) → Type.method(arg, formatter))
                if (PARSE_DATE_TIME.matches(method)) {
                    maybeAddImport("java.time.ZonedDateTime");
                    return PARSE_DATE_TIME_T.apply(getCursor(), m.getCoordinates().replace(),
                            m.getArguments().get(0), m.getSelect());
                }
                if (PARSE_MILLIS.matches(method)) {
                    maybeAddImport("java.time.ZonedDateTime");
                    return PARSE_MILLIS_T.apply(getCursor(), m.getCoordinates().replace(),
                            m.getArguments().get(0), m.getSelect());
                }
                if (PRINT_LONG.matches(method)) {
                    maybeAddImport("java.time.ZonedDateTime");
                    maybeAddImport("java.time.Instant");
                    maybeAddImport("java.time.ZoneId");
                    return PRINT_LONG_T.apply(getCursor(), m.getCoordinates().replace(),
                            m.getArguments().get(0), m.getSelect());
                }
                if (PRINT_READABLE_INSTANT.matches(method)) {
                    return PRINT_READABLE_INSTANT_T.apply(getCursor(), m.getCoordinates().replace(),
                            m.getArguments().get(0), m.getSelect());
                }
                if (WITH_ZONE_UTC.matches(method)) {
                    maybeAddImport("java.time.ZoneOffset");
                    return WITH_ZONE_UTC_T.apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }

                // DateTimeFormat localized methods
                if (SHORT_DATE.matches(method)) {
                    maybeAddImport("java.time.format.DateTimeFormatter");
                    maybeAddImport("java.time.format.FormatStyle");
                    return SHORT_DATE_T.apply(getCursor(), m.getCoordinates().replace());
                }
                if (MEDIUM_DATE.matches(method)) {
                    maybeAddImport("java.time.format.DateTimeFormatter");
                    maybeAddImport("java.time.format.FormatStyle");
                    return MEDIUM_DATE_T.apply(getCursor(), m.getCoordinates().replace());
                }
                if (LONG_DATE.matches(method)) {
                    maybeAddImport("java.time.format.DateTimeFormatter");
                    maybeAddImport("java.time.format.FormatStyle");
                    return LONG_DATE_T.apply(getCursor(), m.getCoordinates().replace());
                }
                if (FULL_DATE.matches(method)) {
                    maybeAddImport("java.time.format.DateTimeFormatter");
                    maybeAddImport("java.time.format.FormatStyle");
                    return FULL_DATE_T.apply(getCursor(), m.getCoordinates().replace());
                }
                if (SHORT_TIME.matches(method)) {
                    maybeAddImport("java.time.format.DateTimeFormatter");
                    maybeAddImport("java.time.format.FormatStyle");
                    return SHORT_TIME_T.apply(getCursor(), m.getCoordinates().replace());
                }
                if (MEDIUM_TIME.matches(method)) {
                    maybeAddImport("java.time.format.DateTimeFormatter");
                    maybeAddImport("java.time.format.FormatStyle");
                    return MEDIUM_TIME_T.apply(getCursor(), m.getCoordinates().replace());
                }
                if (LONG_TIME.matches(method)) {
                    maybeAddImport("java.time.format.DateTimeFormatter");
                    maybeAddImport("java.time.format.FormatStyle");
                    return LONG_TIME_T.apply(getCursor(), m.getCoordinates().replace());
                }
                if (FULL_TIME.matches(method)) {
                    maybeAddImport("java.time.format.DateTimeFormatter");
                    maybeAddImport("java.time.format.FormatStyle");
                    return FULL_TIME_T.apply(getCursor(), m.getCoordinates().replace());
                }
                if (SHORT_DATE_TIME.matches(method)) {
                    maybeAddImport("java.time.format.DateTimeFormatter");
                    maybeAddImport("java.time.format.FormatStyle");
                    return SHORT_DATE_TIME_T.apply(getCursor(), m.getCoordinates().replace());
                }
                if (MEDIUM_DATE_TIME.matches(method)) {
                    maybeAddImport("java.time.format.DateTimeFormatter");
                    maybeAddImport("java.time.format.FormatStyle");
                    return MEDIUM_DATE_TIME_T.apply(getCursor(), m.getCoordinates().replace());
                }
                if (LONG_DATE_TIME.matches(method)) {
                    maybeAddImport("java.time.format.DateTimeFormatter");
                    maybeAddImport("java.time.format.FormatStyle");
                    return LONG_DATE_TIME_T.apply(getCursor(), m.getCoordinates().replace());
                }
                if (FULL_DATE_TIME.matches(method)) {
                    maybeAddImport("java.time.format.DateTimeFormatter");
                    maybeAddImport("java.time.format.FormatStyle");
                    return FULL_DATE_TIME_T.apply(getCursor(), m.getCoordinates().replace());
                }
                return m;
            }
        };
        return Preconditions.check(new UsesType<>("org.joda.time.format.*", true), visitor);
    }
}
