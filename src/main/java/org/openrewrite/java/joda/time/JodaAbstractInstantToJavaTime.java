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
public class JodaAbstractInstantToJavaTime extends Recipe {
    String displayName = "Migrate Joda-Time `AbstractInstant` to Java time";

    String description = "Migrates Joda-Time `AbstractInstant` method calls to their Java time equivalents, for both " +
            "date times and instants.";

    private static final MethodMatcher IS_AFTER_LONG = new MethodMatcher("org.joda.time.base.AbstractInstant isAfter(long)", true);
    private static final MethodMatcher IS_BEFORE_LONG = new MethodMatcher("org.joda.time.base.AbstractInstant isBefore(long)", true);
    private static final MethodMatcher IS_EQUAL_LONG = new MethodMatcher("org.joda.time.base.AbstractInstant isEqual(long)", true);
    private static final MethodMatcher IS_AFTER_NOW = new MethodMatcher("org.joda.time.base.AbstractInstant isAfterNow()", true);
    private static final MethodMatcher IS_BEFORE_NOW = new MethodMatcher("org.joda.time.base.AbstractInstant isBeforeNow()", true);
    private static final MethodMatcher TO_DATE = new MethodMatcher("org.joda.time.base.AbstractInstant toDate()", true);
    private static final MethodMatcher TO_STRING_FORMATTER = new MethodMatcher("org.joda.time.base.AbstractInstant toString(org.joda.time.format.DateTimeFormatter)", true);
    private static final MethodMatcher TO_INSTANT = new MethodMatcher("org.joda.time.base.AbstractInstant toInstant()", true);
    private static final MethodMatcher TO_DATE_TIME = new MethodMatcher("org.joda.time.base.AbstractInstant toDateTime()", true);
    private static final MethodMatcher TO_DATE_TIME_ISO = new MethodMatcher("org.joda.time.base.AbstractInstant toDateTimeISO()", true);
    private static final MethodMatcher TO_DATE_TIME_ZONE = new MethodMatcher("org.joda.time.base.AbstractInstant toDateTime(org.joda.time.DateTimeZone)", true);
    private static final MethodMatcher GET_MILLIS = new MethodMatcher("org.joda.time.base.BaseDateTime getMillis()");
    private static final MethodMatcher GET_MILLIS_READABLE = new MethodMatcher("org.joda.time.ReadableInstant getMillis()");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("org.joda.time.*", true), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                Expression select = m.getSelect();
                // a `ReadableInstant` becomes a `java.time.Instant`
                boolean isInstant = select != null && (JodaTimeTypes.isInstant(select.getType()) ||
                        TypeUtils.isOfClassType(select.getType(), "org.joda.time.ReadableInstant"));
                boolean isDateTime = select != null && JodaTimeTypes.isDateTime(select.getType());
                if (!isInstant && !isDateTime) {
                    return m;
                }
                String receiver = isInstant ? "#{any(java.time.Instant)}" : "#{any(java.time.ZonedDateTime)}";
                String instant = isInstant ? receiver : receiver + ".toInstant()";
                if (IS_AFTER_LONG.matches(method)) {
                    return replace(m, instant + ".isAfter(Instant.ofEpochMilli(#{any(long)}))", select, m.getArguments().get(0));
                }
                if (IS_BEFORE_LONG.matches(method)) {
                    return replace(m, instant + ".isBefore(Instant.ofEpochMilli(#{any(long)}))", select, m.getArguments().get(0));
                }
                if (IS_EQUAL_LONG.matches(method)) {
                    return replace(m, instant + ".equals(Instant.ofEpochMilli(#{any(long)}))", select, m.getArguments().get(0));
                }
                if (IS_AFTER_NOW.matches(method)) {
                    return replace(m, receiver + ".isAfter(" + (isInstant ? "Instant" : "ZonedDateTime") + ".now())", select);
                }
                if (IS_BEFORE_NOW.matches(method)) {
                    return replace(m, receiver + ".isBefore(" + (isInstant ? "Instant" : "ZonedDateTime") + ".now())", select);
                }
                if (TO_DATE.matches(method)) {
                    return replace(m, "Date.from(" + instant + ")", select);
                }
                if (TO_STRING_FORMATTER.matches(method)) {
                    // Joda-Time prints an `Instant` in UTC, unless the formatter has a zone
                    return replace(m, receiver + (isInstant ? ".atZone(ZoneOffset.UTC)" : "") + ".format(#{any(java.time.format.DateTimeFormatter)})",
                            select, m.getArguments().get(0));
                }
                if (TO_INSTANT.matches(method)) {
                    return replace(m, instant, select);
                }
                if (TO_DATE_TIME.matches(method) || TO_DATE_TIME_ISO.matches(method)) {
                    return replace(m, isInstant ? receiver + ".atZone(ZoneId.systemDefault())" : receiver, select);
                }
                if (TO_DATE_TIME_ZONE.matches(method)) {
                    return replace(m, receiver + (isInstant ? ".atZone" : ".withZoneSameInstant") + "(#{any(java.time.ZoneId)})",
                            select, m.getArguments().get(0));
                }
                if (GET_MILLIS.matches(method) || GET_MILLIS_READABLE.matches(method)) {
                    return replace(m, instant + ".toEpochMilli()", select);
                }
                return m;
            }

            private J replace(J.MethodInvocation m, String code, Object... parameters) {
                String[] imports = {"java.time.Instant", "java.time.ZonedDateTime", "java.time.ZoneId", "java.time.ZoneOffset", "java.util.Date"};
                for (String type : imports) {
                    maybeAddImport(type);
                }
                return JavaTemplate.builder(code).imports(imports).build()
                        .apply(getCursor(), m.getCoordinates().replace(), parameters);
            }
        });
    }
}
