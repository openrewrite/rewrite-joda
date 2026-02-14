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
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

@Value
@EqualsAndHashCode(callSuper = false)
public class JodaDateTimeZoneToJavaTime extends Recipe {
    @Getter
    String displayName = "Migrate Joda-Time DateTimeZone to Java time";

    @Getter
    String description = "Migrates `org.joda.time.DateTimeZone` method calls to `java.time.ZoneOffset` and `java.time.ZoneId`.";

    private static final MethodMatcher FOR_OFFSET_HOURS = new MethodMatcher("org.joda.time.DateTimeZone forOffsetHours(int)");
    private static final MethodMatcher FOR_OFFSET_HOURS_MINUTES = new MethodMatcher("org.joda.time.DateTimeZone forOffsetHoursMinutes(int, int)");
    private static final MethodMatcher FOR_TIMEZONE = new MethodMatcher("org.joda.time.DateTimeZone forTimeZone(java.util.TimeZone)");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("org.joda.time.DateTimeZone", true), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (FOR_OFFSET_HOURS.matches(method)) {
                    maybeAddImport("java.time.ZoneOffset");
                    return JavaTemplate.builder("ZoneOffset.ofHours(#{any(int)})")
                            .imports("java.time.ZoneOffset").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getArguments().get(0));
                }
                if (FOR_OFFSET_HOURS_MINUTES.matches(method)) {
                    maybeAddImport("java.time.ZoneOffset");
                    return JavaTemplate.builder("ZoneOffset.ofHoursMinutes(#{any(int)}, #{any(int)})")
                            .imports("java.time.ZoneOffset").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                                    m.getArguments().get(0), m.getArguments().get(1));
                }
                if (FOR_TIMEZONE.matches(method)) {
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("#{any(java.util.TimeZone)}.toZoneId()").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getArguments().get(0));
                }
                return m;
            }

            @Override
            public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                J.FieldAccess f = (J.FieldAccess) super.visitFieldAccess(fieldAccess, ctx);
                if ("UTC".equals(f.getName().getSimpleName()) &&
                        TypeUtils.isOfClassType(f.getTarget().getType(), "org.joda.time.DateTimeZone")) {
                    maybeAddImport("java.time.ZoneOffset");
                    JavaType zoneOffsetType = JavaType.buildType("java.time.ZoneOffset");
                    return f.withTarget(((J.Identifier) f.getTarget())
                                    .withSimpleName("ZoneOffset")
                                    .withType(zoneOffsetType))
                            .withType(zoneOffsetType);
                }
                return f;
            }
        });
    }
}
