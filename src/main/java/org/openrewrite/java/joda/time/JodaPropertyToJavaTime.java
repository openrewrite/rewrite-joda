/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class JodaPropertyToJavaTime extends Recipe {
    String displayName = "Migrate Joda-Time property idioms to Java time";

    String description = "Migrates the common uses of Joda-Time properties, such as `dayOfMonth().withMaximumValue()`, " +
            "`hourOfDay().roundFloorCopy()` and `monthOfYear().getAsText(locale)`, to their `java.time` equivalents.";

    private static final MethodMatcher PROPERTY = new MethodMatcher("org.joda.time.* *()");
    private static final MethodMatcher PROPERTY_METHOD = new MethodMatcher("org.joda.time..* *(..)");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(PROPERTY), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (!PROPERTY_METHOD.matches(m) || !(m.getSelect() instanceof J.MethodInvocation)) {
                    return m;
                }
                J.MethodInvocation property = (J.MethodInvocation) m.getSelect();
                String targetType = property.getSelect() == null ? null : JodaTimeTypes.javaTimeType(property.getSelect().getType());
                if (!PROPERTY.matches(property) || targetType == null) {
                    return m;
                }
                boolean hasDate = !"java.time.LocalTime".equals(targetType);
                boolean hasTime = !"java.time.LocalDate".equals(targetType);
                String field = property.getSimpleName();
                String code = null;
                List<String> imports = new ArrayList<>();
                List<Expression> parameters = new ArrayList<>();
                parameters.add(property.getSelect());
                switch (m.getSimpleName()) {
                    case "get":
                        code = getter(targetType, field, hasDate, hasTime);
                        break;
                    case "withMaximumValue":
                        if (hasDate && "dayOfMonth".equals(field)) {
                            code = "#{any(" + targetType + ")}.with(TemporalAdjusters.lastDayOfMonth())";
                            imports.add("java.time.temporal.TemporalAdjusters");
                        } else if (hasDate && "dayOfYear".equals(field)) {
                            code = "#{any(" + targetType + ")}.with(TemporalAdjusters.lastDayOfYear())";
                            imports.add("java.time.temporal.TemporalAdjusters");
                        } else if (hasDate && "monthOfYear".equals(field)) {
                            code = "#{any(" + targetType + ")}.withMonth(12)";
                        } else if (hasDate && "dayOfWeek".equals(field)) {
                            code = "#{any(" + targetType + ")}.with(ChronoField.DAY_OF_WEEK, 7)";
                            imports.add("java.time.temporal.ChronoField");
                        } else if (hasTime && "hourOfDay".equals(field)) {
                            code = "#{any(" + targetType + ")}.withHour(23)";
                        } else if (hasTime && "minuteOfHour".equals(field)) {
                            code = "#{any(" + targetType + ")}.withMinute(59)";
                        } else if (hasTime && "secondOfMinute".equals(field)) {
                            code = "#{any(" + targetType + ")}.withSecond(59)";
                        }
                        break;
                    case "withMinimumValue":
                        if (hasDate && "dayOfMonth".equals(field)) {
                            code = "#{any(" + targetType + ")}.withDayOfMonth(1)";
                        } else if (hasDate && "dayOfYear".equals(field)) {
                            code = "#{any(" + targetType + ")}.withDayOfYear(1)";
                        } else if (hasDate && "monthOfYear".equals(field)) {
                            code = "#{any(" + targetType + ")}.withMonth(1)";
                        } else if (hasDate && "dayOfWeek".equals(field)) {
                            code = "#{any(" + targetType + ")}.with(ChronoField.DAY_OF_WEEK, 1)";
                            imports.add("java.time.temporal.ChronoField");
                        } else if (hasTime && "hourOfDay".equals(field)) {
                            code = "#{any(" + targetType + ")}.withHour(0)";
                        } else if (hasTime && "minuteOfHour".equals(field)) {
                            code = "#{any(" + targetType + ")}.withMinute(0)";
                        } else if (hasTime && "secondOfMinute".equals(field)) {
                            code = "#{any(" + targetType + ")}.withSecond(0)";
                        }
                        break;
                    case "getMaximumValue":
                        String localDate = "java.time.LocalDate".equals(targetType) ? "" : ".toLocalDate()";
                        if (hasDate && "dayOfMonth".equals(field)) {
                            code = "#{any(" + targetType + ")}" + localDate + ".lengthOfMonth()";
                        } else if (hasDate && "dayOfYear".equals(field)) {
                            code = "#{any(" + targetType + ")}" + localDate + ".lengthOfYear()";
                        }
                        break;
                    case "roundFloorCopy":
                        String unit = "dayOfMonth".equals(field) && hasDate && hasTime ? "DAYS" :
                                "hourOfDay".equals(field) && hasTime ? "HOURS" :
                                        "minuteOfHour".equals(field) && hasTime ? "MINUTES" :
                                                "secondOfMinute".equals(field) && hasTime ? "SECONDS" : null;
                        if (unit != null) {
                            code = "#{any(" + targetType + ")}.truncatedTo(ChronoUnit." + unit + ")";
                            imports.add("java.time.temporal.ChronoUnit");
                        }
                        break;
                    case "getAsText":
                    case "getAsShortText":
                        String text = "monthOfYear".equals(field) && hasDate ? "getMonth()" :
                                "dayOfWeek".equals(field) && hasDate ? "getDayOfWeek()" : null;
                        if (text != null && m.getArguments().size() == 1) {
                            String style = "getAsText".equals(m.getSimpleName()) ? "FULL" : "SHORT";
                            imports.add("java.time.format.TextStyle");
                            imports.add("java.util.Locale");
                            if (m.getArguments().get(0) instanceof J.Empty) {
                                code = "#{any(" + targetType + ")}." + text + ".getDisplayName(TextStyle." + style + ", Locale.getDefault())";
                            } else {
                                code = "#{any(" + targetType + ")}." + text + ".getDisplayName(TextStyle." + style + ", #{any(java.util.Locale)})";
                                parameters.add(m.getArguments().get(0));
                            }
                        }
                        break;
                    default:
                        break;
                }
                if (code == null) {
                    return m;
                }
                for (String type : imports) {
                    maybeAddImport(type);
                }
                return JavaTemplate.builder(code)
                        .imports(imports.toArray(new String[0])).build()
                        .apply(getCursor(), m.getCoordinates().replace(), parameters.toArray());
            }

            private @Nullable String getter(String targetType, String field, boolean hasDate, boolean hasTime) {
                String target = "#{any(" + targetType + ")}.";
                if (hasDate) {
                    switch (field) {
                        case "year":
                            return target + "getYear()";
                        case "monthOfYear":
                            return target + "getMonthValue()";
                        case "dayOfMonth":
                            return target + "getDayOfMonth()";
                        case "dayOfYear":
                            return target + "getDayOfYear()";
                        case "dayOfWeek":
                            return target + "getDayOfWeek().getValue()";
                        default:
                            break;
                    }
                }
                if (hasTime) {
                    switch (field) {
                        case "hourOfDay":
                            return target + "getHour()";
                        case "minuteOfHour":
                            return target + "getMinute()";
                        case "secondOfMinute":
                            return target + "getSecond()";
                        default:
                            break;
                    }
                }
                return null;
            }
        });
    }
}
