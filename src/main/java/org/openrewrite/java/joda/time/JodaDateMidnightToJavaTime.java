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

@Value
@EqualsAndHashCode(callSuper = false)
public class JodaDateMidnightToJavaTime extends Recipe {
    String displayName = "Migrate Joda-Time `DateMidnight` to Java time";

    String description = "Migrates `org.joda.time.DateMidnight` constructors and `now()` calls to `java.time.LocalDate.atStartOfDay(...)`.";

    private static final MethodMatcher CONSTRUCTOR = new MethodMatcher("org.joda.time.DateMidnight <constructor>()");
    private static final MethodMatcher CONSTRUCTOR_ZONE = new MethodMatcher("org.joda.time.DateMidnight <constructor>(org.joda.time.DateTimeZone)");
    private static final MethodMatcher CONSTRUCTOR_YMD = new MethodMatcher("org.joda.time.DateMidnight <constructor>(int, int, int)");
    private static final MethodMatcher CONSTRUCTOR_YMD_ZONE = new MethodMatcher("org.joda.time.DateMidnight <constructor>(int, int, int, org.joda.time.DateTimeZone)");
    private static final MethodMatcher NOW = new MethodMatcher("org.joda.time.DateMidnight now()");
    private static final MethodMatcher NOW_ZONE = new MethodMatcher("org.joda.time.DateMidnight now(org.joda.time.DateTimeZone)");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("org.joda.time.DateMidnight", true), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass nc = (J.NewClass) super.visitNewClass(newClass, ctx);
                if (CONSTRUCTOR.matches(newClass)) {
                    return replace(nc, "LocalDate.now().atStartOfDay(ZoneId.systemDefault())");
                }
                if (CONSTRUCTOR_ZONE.matches(newClass) && JodaDateTimeToJavaTime.isSimple(nc.getArguments().get(0))) {
                    return replace(nc, "LocalDate.now(#{any(java.time.ZoneId)}).atStartOfDay(#{any(java.time.ZoneId)})",
                            nc.getArguments().get(0), nc.getArguments().get(0));
                }
                if (CONSTRUCTOR_YMD.matches(newClass)) {
                    return replace(nc, "LocalDate.of(#{any(int)}, #{any(int)}, #{any(int)}).atStartOfDay(ZoneId.systemDefault())",
                            nc.getArguments().get(0), nc.getArguments().get(1), nc.getArguments().get(2));
                }
                if (CONSTRUCTOR_YMD_ZONE.matches(newClass)) {
                    return replace(nc, "LocalDate.of(#{any(int)}, #{any(int)}, #{any(int)}).atStartOfDay(#{any(java.time.ZoneId)})",
                            nc.getArguments().toArray());
                }
                return nc;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (NOW.matches(method)) {
                    return replace(m, "LocalDate.now().atStartOfDay(ZoneId.systemDefault())");
                }
                if (NOW_ZONE.matches(method) && JodaDateTimeToJavaTime.isSimple(m.getArguments().get(0))) {
                    return replace(m, "LocalDate.now(#{any(java.time.ZoneId)}).atStartOfDay(#{any(java.time.ZoneId)})",
                            m.getArguments().get(0), m.getArguments().get(0));
                }
                return m;
            }

            private J replace(Expression expression, String code, Object... parameters) {
                maybeAddImport("java.time.LocalDate");
                maybeAddImport("java.time.ZoneId");
                return JavaTemplate.builder(code)
                        .imports("java.time.LocalDate", "java.time.ZoneId").build()
                        .apply(getCursor(), expression.getCoordinates().replace(), parameters);
            }
        });
    }
}
