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
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Arrays;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class JodaTimePeriodToJavaTime extends Recipe {
    String displayName = "Migrate Joda-Time `Days`, `Hours`, `Minutes`, `Seconds` to Java time";

    String description = "Migrates `org.joda.time.Days`, `Hours`, `Minutes`, and `Seconds` to `java.time.temporal.ChronoUnit` and `java.time.Duration`.";

    // Days
    private static final MethodMatcher DAYS_BETWEEN = new MethodMatcher("org.joda.time.Days daysBetween(org.joda.time.ReadableInstant, org.joda.time.ReadableInstant)", true);
    private static final MethodMatcher DAYS_BETWEEN_PARTIAL = new MethodMatcher("org.joda.time.Days daysBetween(org.joda.time.ReadablePartial, org.joda.time.ReadablePartial)", true);
    private static final MethodMatcher DAYS_IN = new MethodMatcher("org.joda.time.Days daysIn(org.joda.time.ReadableInterval)", true);
    private static final MethodMatcher DAYS_DAYS = new MethodMatcher("org.joda.time.Days days(int)");
    private static final MethodMatcher DAYS_GET_DAYS = new MethodMatcher("org.joda.time.Days getDays()");
    private static final MethodMatcher DAYS_TO_STANDARD_DURATION = new MethodMatcher("org.joda.time.Days toStandardDuration()");
    // Hours
    private static final MethodMatcher HOURS_BETWEEN = new MethodMatcher("org.joda.time.Hours hoursBetween(org.joda.time.ReadableInstant, org.joda.time.ReadableInstant)", true);
    private static final MethodMatcher HOURS_BETWEEN_PARTIAL = new MethodMatcher("org.joda.time.Hours hoursBetween(org.joda.time.ReadablePartial, org.joda.time.ReadablePartial)", true);
    private static final MethodMatcher HOURS_HOURS = new MethodMatcher("org.joda.time.Hours hours(int)");
    private static final MethodMatcher HOURS_GET_HOURS = new MethodMatcher("org.joda.time.Hours getHours()");
    private static final MethodMatcher HOURS_TO_STANDARD_DURATION = new MethodMatcher("org.joda.time.Hours toStandardDuration()");
    // Minutes
    private static final MethodMatcher MINUTES_BETWEEN = new MethodMatcher("org.joda.time.Minutes minutesBetween(org.joda.time.ReadableInstant, org.joda.time.ReadableInstant)", true);
    private static final MethodMatcher MINUTES_BETWEEN_PARTIAL = new MethodMatcher("org.joda.time.Minutes minutesBetween(org.joda.time.ReadablePartial, org.joda.time.ReadablePartial)", true);
    private static final MethodMatcher MINUTES_MINUTES = new MethodMatcher("org.joda.time.Minutes minutes(int)");
    private static final MethodMatcher MINUTES_GET_MINUTES = new MethodMatcher("org.joda.time.Minutes getMinutes()");
    private static final MethodMatcher MINUTES_TO_STANDARD_DURATION = new MethodMatcher("org.joda.time.Minutes toStandardDuration()");
    // Seconds
    private static final MethodMatcher SECONDS_BETWEEN = new MethodMatcher("org.joda.time.Seconds secondsBetween(org.joda.time.ReadableInstant, org.joda.time.ReadableInstant)", true);
    private static final MethodMatcher SECONDS_BETWEEN_PARTIAL = new MethodMatcher("org.joda.time.Seconds secondsBetween(org.joda.time.ReadablePartial, org.joda.time.ReadablePartial)", true);
    private static final MethodMatcher SECONDS_SECONDS = new MethodMatcher("org.joda.time.Seconds seconds(int)");
    private static final MethodMatcher SECONDS_GET_SECONDS = new MethodMatcher("org.joda.time.Seconds getSeconds()");
    private static final MethodMatcher SECONDS_TO_STANDARD_DURATION = new MethodMatcher("org.joda.time.Seconds toStandardDuration()");

    private static final List<String> JODA_PERIOD_TYPES = Arrays.asList(
            "org.joda.time.Days", "org.joda.time.Hours",
            "org.joda.time.Minutes", "org.joda.time.Seconds"
    );

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(
                        new UsesType<>("org.joda.time.Days", true),
                        new UsesType<>("org.joda.time.Hours", true),
                        new UsesType<>("org.joda.time.Minutes", true),
                        new UsesType<>("org.joda.time.Seconds", true)
                ),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

                        // Handle chained patterns like Days.daysBetween(a, b).getDays()
                        if (isGetValueCall(m) && m.getSelect() instanceof J.MethodInvocation) {
                            J.MethodInvocation inner = (J.MethodInvocation) m.getSelect();
                            String outerType = getDeclaringTypeName(m);
                            String innerType = getDeclaringTypeName(inner);
                            if (outerType == null || !outerType.equals(innerType)) {
                                return m;
                            }
                            String unit = getChronoUnit(outerType);
                            // Pattern: Days.daysBetween(a, b).getDays() -> (int) ChronoUnit.DAYS.between(a, b)
                            if (isBetweenCall(inner)) {
                                maybeAddImport("java.time.temporal.ChronoUnit");
                                removeJodaPeriodImports();
                                return JavaTemplate.builder("(int) ChronoUnit." + unit + ".between(#{any()}, #{any()})")
                                        .imports("java.time.temporal.ChronoUnit").build()
                                        .apply(getCursor(), m.getCoordinates().replace(),
                                                inner.getArguments().get(0), inner.getArguments().get(1));
                            }
                            // Pattern: Days.daysIn(interval).getDays() -> (int) ChronoUnit.DAYS.between(interval.getStart(), interval.getEnd())
                            if (DAYS_IN.matches(inner)) {
                                maybeAddImport("java.time.temporal.ChronoUnit");
                                removeJodaPeriodImports();
                                return JavaTemplate.builder("(int) ChronoUnit.DAYS.between(#{any()}.getStart(), #{any()}.getEnd())")
                                        .imports("java.time.temporal.ChronoUnit").build()
                                        .apply(getCursor(), m.getCoordinates().replace(),
                                                inner.getArguments().get(0), inner.getArguments().get(0));
                            }
                        }

                        // Handle toStandardDuration() chained on factory or between call
                        if (isToStandardDuration(m) && m.getSelect() instanceof J.MethodInvocation) {
                            J.MethodInvocation inner = (J.MethodInvocation) m.getSelect();
                            String outerType = getDeclaringTypeName(m);
                            String innerType = getDeclaringTypeName(inner);
                            if (outerType == null || !outerType.equals(innerType)) {
                                return m;
                            }
                            String unit = getChronoUnit(outerType);
                            String durationMethod = getDurationOfMethodFromUnit(unit);
                            if (isFactoryCall(inner)) {
                                maybeAddImport("java.time.Duration");
                                removeJodaPeriodImports();
                                return JavaTemplate.builder("Duration." + durationMethod + "(#{any(int)})")
                                        .imports("java.time.Duration").build()
                                        .apply(getCursor(), m.getCoordinates().replace(), inner.getArguments().get(0));
                            }
                            // Days.daysBetween(a, b).toStandardDuration() -> Duration.ofDays(ChronoUnit.DAYS.between(a, b))
                            if (isBetweenCall(inner)) {
                                maybeAddImport("java.time.Duration");
                                maybeAddImport("java.time.temporal.ChronoUnit");
                                removeJodaPeriodImports();
                                return JavaTemplate.builder("Duration." + durationMethod + "(ChronoUnit." + unit + ".between(#{any()}, #{any()}))")
                                        .imports("java.time.Duration", "java.time.temporal.ChronoUnit").build()
                                        .apply(getCursor(), m.getCoordinates().replace(),
                                                inner.getArguments().get(0), inner.getArguments().get(1));
                            }
                        }

                        // Standalone factory: Days.days(n) -> Duration.ofDays(n)
                        if (isFactoryCall(m)) {
                            Object parentValue = getCursor().getParentTreeCursor().getValue();
                            if (parentValue instanceof J.MethodInvocation &&
                                    isToStandardDuration((J.MethodInvocation) parentValue)) {
                                return m;
                            }
                            String type = getDeclaringTypeName(m);
                            if (type == null) {
                                return m;
                            }
                            String unit = getChronoUnit(type);
                            String durationMethod = getDurationOfMethodFromUnit(unit);
                            maybeAddImport("java.time.Duration");
                            removeJodaPeriodImports();
                            return JavaTemplate.builder("Duration." + durationMethod + "(#{any(int)})")
                                    .imports("java.time.Duration").build()
                                    .apply(getCursor(), m.getCoordinates().replace(), m.getArguments().get(0));
                        }

                        return m;
                    }

                    private void removeJodaPeriodImports() {
                        for (String type : JODA_PERIOD_TYPES) {
                            maybeRemoveImport(type);
                        }
                    }

                    private String getDeclaringTypeName(J.MethodInvocation m) {
                        if (m.getMethodType() == null) {
                            return null;
                        }
                        JavaType.FullyQualified type = TypeUtils.asFullyQualified(m.getMethodType().getDeclaringType());
                        return type != null ? type.getFullyQualifiedName() : null;
                    }

                    private boolean isGetValueCall(J.MethodInvocation m) {
                        return DAYS_GET_DAYS.matches(m) || HOURS_GET_HOURS.matches(m) ||
                               MINUTES_GET_MINUTES.matches(m) || SECONDS_GET_SECONDS.matches(m);
                    }

                    private boolean isBetweenCall(J.MethodInvocation m) {
                        return DAYS_BETWEEN.matches(m) || DAYS_BETWEEN_PARTIAL.matches(m) ||
                               HOURS_BETWEEN.matches(m) || HOURS_BETWEEN_PARTIAL.matches(m) ||
                               MINUTES_BETWEEN.matches(m) || MINUTES_BETWEEN_PARTIAL.matches(m) ||
                               SECONDS_BETWEEN.matches(m) || SECONDS_BETWEEN_PARTIAL.matches(m);
                    }

                    private boolean isFactoryCall(J.MethodInvocation m) {
                        return DAYS_DAYS.matches(m) || HOURS_HOURS.matches(m) ||
                               MINUTES_MINUTES.matches(m) || SECONDS_SECONDS.matches(m);
                    }

                    private boolean isToStandardDuration(J.MethodInvocation m) {
                        return DAYS_TO_STANDARD_DURATION.matches(m) || HOURS_TO_STANDARD_DURATION.matches(m) ||
                               MINUTES_TO_STANDARD_DURATION.matches(m) || SECONDS_TO_STANDARD_DURATION.matches(m);
                    }

                    private String getChronoUnit(String fullyQualifiedTypeName) {
                        switch (fullyQualifiedTypeName) {
                            case "org.joda.time.Days":
                                return "DAYS";
                            case "org.joda.time.Hours":
                                return "HOURS";
                            case "org.joda.time.Minutes":
                                return "MINUTES";
                            case "org.joda.time.Seconds":
                                return "SECONDS";
                            default:
                                return "DAYS";
                        }
                    }

                    private String getDurationOfMethodFromUnit(String unit) {
                        switch (unit) {
                            case "DAYS":
                                return "ofDays";
                            case "HOURS":
                                return "ofHours";
                            case "MINUTES":
                                return "ofMinutes";
                            case "SECONDS":
                                return "ofSeconds";
                            default:
                                return "ofDays";
                        }
                    }
                }
        );
    }
}
