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
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

@Value
@EqualsAndHashCode(callSuper = false)
public class JodaDateTimeZoneToJavaTime extends Recipe {
    String displayName = "Migrate Joda-Time `DateTimeZone` to Java time";

    String description = "Migrates `org.joda.time.DateTimeZone` method calls to `java.time.ZoneOffset` and `java.time.ZoneId`.";

    private static final MethodMatcher FOR_OFFSET_HOURS = new MethodMatcher("org.joda.time.DateTimeZone forOffsetHours(int)");
    private static final MethodMatcher FOR_OFFSET_HOURS_MINUTES = new MethodMatcher("org.joda.time.DateTimeZone forOffsetHoursMinutes(int, int)");
    private static final MethodMatcher FOR_OFFSET_MILLIS = new MethodMatcher("org.joda.time.DateTimeZone forOffsetMillis(int)");
    private static final MethodMatcher FOR_TIMEZONE = new MethodMatcher("org.joda.time.DateTimeZone forTimeZone(java.util.TimeZone)");
    private static final MethodMatcher GET_DEFAULT = new MethodMatcher("org.joda.time.DateTimeZone getDefault()");
    private static final MethodMatcher GET_AVAILABLE_IDS = new MethodMatcher("org.joda.time.DateTimeZone getAvailableIDs()");
    private static final MethodMatcher GET_ID = new MethodMatcher("org.joda.time.DateTimeZone getID()");
    private static final MethodMatcher TO_TIME_ZONE = new MethodMatcher("org.joda.time.DateTimeZone toTimeZone()");
    private static final MethodMatcher GET_OFFSET_LONG = new MethodMatcher("org.joda.time.DateTimeZone getOffset(long)");
    private static final MethodMatcher GET_OFFSET_INSTANT = new MethodMatcher("org.joda.time.DateTimeZone getOffset(org.joda.time.ReadableInstant)");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("org.joda.time.DateTimeZone", true), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitImport(J.Import _import, ExecutionContext ctx) {
                if (_import.isStatic() && "UTC".equals(_import.getQualid().getSimpleName()) &&
                        "org.joda.time.DateTimeZone".equals(_import.getTypeName())) {
                    return _import.withQualid(TypeTree.<J.FieldAccess>build("java.time.ZoneOffset.UTC")
                            .withPrefix(_import.getQualid().getPrefix()));
                }
                return _import;
            }

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
                if (FOR_OFFSET_MILLIS.matches(method)) {
                    maybeAddImport("java.time.ZoneOffset");
                    return JavaTemplate.builder("ZoneOffset.ofTotalSeconds(#{any(int)} / 1000)")
                            .imports("java.time.ZoneOffset").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getArguments().get(0));
                }
                if (FOR_TIMEZONE.matches(method)) {
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.apply("#{any(java.util.TimeZone)}.toZoneId()", getCursor(), m.getCoordinates().replace(), m.getArguments().get(0));
                }
                if (GET_DEFAULT.matches(method)) {
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("ZoneId.systemDefault()")
                            .imports("java.time.ZoneId").build()
                            .apply(getCursor(), m.getCoordinates().replace());
                }
                if (GET_AVAILABLE_IDS.matches(method)) {
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("ZoneId.getAvailableZoneIds()")
                            .imports("java.time.ZoneId").build()
                            .apply(getCursor(), m.getCoordinates().replace());
                }
                if (GET_ID.matches(method)) {
                    return JavaTemplate.apply("#{any(java.time.ZoneId)}.getId()", getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (TO_TIME_ZONE.matches(method)) {
                    maybeAddImport("java.util.TimeZone");
                    return JavaTemplate.builder("TimeZone.getTimeZone(#{any(java.time.ZoneId)})")
                            .imports("java.util.TimeZone").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (GET_OFFSET_LONG.matches(method)) {
                    maybeAddImport("java.time.Instant");
                    return JavaTemplate.builder("#{any(java.time.ZoneId)}.getRules().getOffset(Instant.ofEpochMilli(#{any(long)})).getTotalSeconds() * 1000")
                            .imports("java.time.Instant").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect(), m.getArguments().get(0));
                }
                if (GET_OFFSET_INSTANT.matches(method) && JodaTimeTypes.isDateTime(m.getArguments().get(0).getType())) {
                    return JavaTemplate.apply("#{any(java.time.ZoneId)}.getRules().getOffset(#{any(java.time.ZonedDateTime)}.toInstant()).getTotalSeconds() * 1000",
                            getCursor(), m.getCoordinates().replace(), m.getSelect(), m.getArguments().get(0));
                }
                return m;
            }

            @Override
            public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                J.FieldAccess f = (J.FieldAccess) super.visitFieldAccess(fieldAccess, ctx);
                if ("UTC".equals(f.getSimpleName()) && TypeUtils.isOfClassType(f.getTarget().getType(), "org.joda.time.DateTimeZone") &&
                        getCursor().firstEnclosing(J.Import.class) == null) {
                    maybeAddImport("java.time.ZoneOffset");
                    return JavaTemplate.builder("ZoneOffset.UTC")
                            .imports("java.time.ZoneOffset").build()
                            .apply(getCursor(), f.getCoordinates().replace());
                }
                return f;
            }

            @Override
            public J visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                JavaType.Variable fieldType = identifier.getFieldType();
                if (fieldType != null && "UTC".equals(fieldType.getName()) &&
                        TypeUtils.isOfClassType(fieldType.getOwner(), "org.joda.time.DateTimeZone")) {
                    JavaType.FullyQualified zoneOffset = JavaType.ShallowClass.build("java.time.ZoneOffset");
                    return identifier.withType(zoneOffset).withFieldType(fieldType.withOwner(zoneOffset).withType(zoneOffset));
                }
                return super.visitIdentifier(identifier, ctx);
            }
        });
    }
}
