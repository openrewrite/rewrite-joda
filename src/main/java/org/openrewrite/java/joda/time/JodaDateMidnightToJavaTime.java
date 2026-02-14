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
public class JodaDateMidnightToJavaTime extends Recipe {
    @Getter
    String displayName = "Migrate Joda-Time DateMidnight to Java time";

    @Getter
    String description = "Migrates `org.joda.time.DateMidnight` constructor and `now()` calls to `java.time.LocalDate.now().atStartOfDay(...)`.";

    private static final MethodMatcher CONSTRUCTOR = new MethodMatcher("org.joda.time.DateMidnight <constructor>()");
    private static final MethodMatcher NOW = new MethodMatcher("org.joda.time.DateMidnight now()");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("org.joda.time.DateMidnight", true), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass nc = (J.NewClass) super.visitNewClass(newClass, ctx);
                if (CONSTRUCTOR.matches(newClass)) {
                    maybeAddImport("java.time.LocalDate");
                    maybeAddImport("java.time.ZoneOffset");
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate
                            .builder("LocalDate.now().atStartOfDay(ZoneOffset.of(ZoneId.systemDefault().getId()))")
                            .imports("java.time.LocalDate", "java.time.ZoneOffset", "java.time.ZoneId")
                            .build()
                            .apply(getCursor(), nc.getCoordinates().replace());
                }
                return nc;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (NOW.matches(method)) {
                    maybeAddImport("java.time.LocalDate");
                    maybeAddImport("java.time.ZoneOffset");
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate
                            .builder("LocalDate.now().atStartOfDay(ZoneOffset.of(ZoneId.systemDefault().getId()))")
                            .imports("java.time.LocalDate", "java.time.ZoneOffset", "java.time.ZoneId")
                            .build()
                            .apply(getCursor(), m.getCoordinates().replace());
                }
                return m;
            }
        });
    }
}
