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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Value
@EqualsAndHashCode(callSuper = false)
public class JodaFormatterToJavaTime extends Recipe {
    String displayName = "Migrate Joda-Time formatter to Java time";

    String description = "Migrates Joda-Time `DateTimeFormatter`, `DateTimeFormat` and `ISODateTimeFormat` method calls to " +
            "their Java time equivalents. Patterns are translated where Joda-Time and `java.time` read a pattern letter " +
            "differently, and left alone when there is no exact translation.";

    private static final MethodMatcher FOR_PATTERN = new MethodMatcher("org.joda.time.format.DateTimeFormat forPattern(String)");
    private static final MethodMatcher STYLE = new MethodMatcher("org.joda.time.format.DateTimeFormat *()");
    private static final MethodMatcher ISO = new MethodMatcher("org.joda.time.format.ISODateTimeFormat *()");

    private static final MethodMatcher PARSE_DATE_TIME = new MethodMatcher("org.joda.time.format.DateTimeFormatter parseDateTime(String)");
    private static final MethodMatcher DATE_TIME_PARSE = new MethodMatcher("org.joda.time.DateTime parse(String, org.joda.time.format.DateTimeFormatter)");
    private static final MethodMatcher PARSE_MILLIS = new MethodMatcher("org.joda.time.format.DateTimeFormatter parseMillis(String)");
    private static final MethodMatcher PARSE_LOCAL_DATE = new MethodMatcher("org.joda.time.format.DateTimeFormatter parseLocalDate(String)");
    private static final MethodMatcher PARSE_LOCAL_TIME = new MethodMatcher("org.joda.time.format.DateTimeFormatter parseLocalTime(String)");
    private static final MethodMatcher PARSE_LOCAL_DATE_TIME = new MethodMatcher("org.joda.time.format.DateTimeFormatter parseLocalDateTime(String)");
    private static final MethodMatcher PRINT_LONG = new MethodMatcher("org.joda.time.format.DateTimeFormatter print(long)");
    private static final MethodMatcher PRINT_READABLE_INSTANT = new MethodMatcher("org.joda.time.format.DateTimeFormatter print(org.joda.time.ReadableInstant)");
    private static final MethodMatcher PRINT_READABLE_PARTIAL = new MethodMatcher("org.joda.time.format.DateTimeFormatter print(org.joda.time.ReadablePartial)");
    private static final MethodMatcher WITH_ZONE_UTC = new MethodMatcher("org.joda.time.format.DateTimeFormatter withZoneUTC()");

    private static final MethodMatcher TO_STRING_PATTERN = new MethodMatcher("org.joda.time..* toString(String)");
    private static final MethodMatcher TO_STRING_PATTERN_LOCALE = new MethodMatcher("org.joda.time..* toString(String, java.util.Locale)");
    private static final MethodMatcher PARTIAL_TO_STRING_FORMATTER = new MethodMatcher("org.joda.time.base.AbstractPartial toString(org.joda.time.format.DateTimeFormatter)", true);

    private static final Map<String, String> STYLES = new HashMap<>();
    private static final Map<String, String> ISO_FORMATS = new HashMap<>();

    static {
        STYLES.put("shortDate", "ofLocalizedDate(FormatStyle.SHORT)");
        STYLES.put("mediumDate", "ofLocalizedDate(FormatStyle.MEDIUM)");
        STYLES.put("longDate", "ofLocalizedDate(FormatStyle.LONG)");
        STYLES.put("fullDate", "ofLocalizedDate(FormatStyle.FULL)");
        STYLES.put("shortTime", "ofLocalizedTime(FormatStyle.SHORT)");
        STYLES.put("mediumTime", "ofLocalizedTime(FormatStyle.MEDIUM)");
        STYLES.put("longTime", "ofLocalizedTime(FormatStyle.LONG)");
        STYLES.put("fullTime", "ofLocalizedTime(FormatStyle.FULL)");
        STYLES.put("shortDateTime", "ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)");
        STYLES.put("mediumDateTime", "ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM)");
        STYLES.put("longDateTime", "ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.LONG)");
        STYLES.put("fullDateTime", "ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.FULL)");

        // Joda-Time always prints the milliseconds of `dateTime()`, where `ISO_OFFSET_DATE_TIME` drops trailing zeros
        ISO_FORMATS.put("date", "DateTimeFormatter.ISO_LOCAL_DATE");
        ISO_FORMATS.put("basicDate", "DateTimeFormatter.ofPattern(\"yyyyMMdd\")");
        ISO_FORMATS.put("dateTime", "DateTimeFormatter.ofPattern(\"yyyy-MM-dd'T'HH:mm:ss.SSSXXX\")");
        ISO_FORMATS.put("dateTimeNoMillis", "DateTimeFormatter.ofPattern(\"yyyy-MM-dd'T'HH:mm:ssXXX\")");
        ISO_FORMATS.put("dateHourMinute", "DateTimeFormatter.ofPattern(\"yyyy-MM-dd'T'HH:mm\")");
        ISO_FORMATS.put("dateHourMinuteSecond", "DateTimeFormatter.ofPattern(\"yyyy-MM-dd'T'HH:mm:ss\")");
        ISO_FORMATS.put("dateHourMinuteSecondMillis", "DateTimeFormatter.ofPattern(\"yyyy-MM-dd'T'HH:mm:ss.SSS\")");
        ISO_FORMATS.put("hourMinute", "DateTimeFormatter.ofPattern(\"HH:mm\")");
        ISO_FORMATS.put("hourMinuteSecond", "DateTimeFormatter.ofPattern(\"HH:mm:ss\")");
        ISO_FORMATS.put("yearMonth", "DateTimeFormatter.ofPattern(\"yyyy-MM\")");

    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("org.joda.time..*", true), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

                if (FOR_PATTERN.matches(method)) {
                    Expression pattern = m.getArguments().get(0);
                    String translated = translatedLiteral(pattern);
                    if (translated != null) {
                        maybeAddImport("java.time.format.DateTimeFormatter");
                        return JavaTemplate.builder("DateTimeFormatter.ofPattern(\"" + escape(translated) + "\")")
                                .imports("java.time.format.DateTimeFormatter").build()
                                .apply(getCursor(), m.getCoordinates().replace());
                    }
                    // a pattern that can not be checked is used as is
                    maybeAddImport("java.time.format.DateTimeFormatter");
                    return JavaTemplate.builder("DateTimeFormatter.ofPattern(#{any(String)})")
                            .imports("java.time.format.DateTimeFormatter").build()
                            .apply(getCursor(), m.getCoordinates().replace(), pattern);
                }
                if (STYLE.matches(method) && STYLES.containsKey(m.getSimpleName())) {
                    maybeAddImport("java.time.format.DateTimeFormatter");
                    maybeAddImport("java.time.format.FormatStyle");
                    return JavaTemplate.builder("DateTimeFormatter." + STYLES.get(m.getSimpleName()))
                            .imports("java.time.format.DateTimeFormatter", "java.time.format.FormatStyle").build()
                            .apply(getCursor(), m.getCoordinates().replace());
                }
                if (ISO.matches(method) && ISO_FORMATS.containsKey(m.getSimpleName())) {
                    maybeAddImport("java.time.format.DateTimeFormatter");
                    maybeRemoveImport("org.joda.time.format.ISODateTimeFormat");
                    return JavaTemplate.builder(ISO_FORMATS.get(m.getSimpleName()))
                            .imports("java.time.format.DateTimeFormatter").build()
                            .apply(getCursor(), m.getCoordinates().replace());
                }

                // Joda-Time fills in the formatter's zone, or the default zone, when the text has none
                if (PARSE_DATE_TIME.matches(method) || PARSE_MILLIS.matches(method)) {
                    return parseZoned(m, m.getArguments().get(0), m.getSelect(), PARSE_MILLIS.matches(method) ? ".toInstant().toEpochMilli()" : "");
                }
                if (DATE_TIME_PARSE.matches(method)) {
                    return parseZoned(m, m.getArguments().get(0), m.getArguments().get(1), "");
                }
                if (PARSE_LOCAL_DATE.matches(method) || PARSE_LOCAL_TIME.matches(method) || PARSE_LOCAL_DATE_TIME.matches(method)) {
                    String type = PARSE_LOCAL_DATE.matches(method) ? "LocalDate" : PARSE_LOCAL_TIME.matches(method) ? "LocalTime" : "LocalDateTime";
                    maybeAddImport("java.time." + type);
                    return JavaTemplate.builder(type + ".parse(#{any(java.lang.String)}, #{any(java.time.format.DateTimeFormatter)})")
                            .imports("java.time." + type).build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getArguments().get(0), m.getSelect());
                }

                if (PRINT_LONG.matches(method)) {
                    maybeAddImport("java.time.ZonedDateTime");
                    maybeAddImport("java.time.Instant");
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate
                            .builder("ZonedDateTime.ofInstant(Instant.ofEpochMilli(#{any(long)}), ZoneId.systemDefault()).format(#{any(java.time.format.DateTimeFormatter)})")
                            .imports("java.time.ZonedDateTime", "java.time.Instant", "java.time.ZoneId").build()
                            .apply(getCursor(), m.getCoordinates().replace(),
                                    m.getArguments().get(0), m.getSelect());
                }
                if (PRINT_READABLE_INSTANT.matches(method)) {
                    Expression instant = m.getArguments().get(0);
                    if (JodaTimeTypes.isInstant(instant.getType())) {
                        // Joda-Time prints an `Instant` in UTC, unless the formatter has a zone
                        maybeAddImport("java.time.ZoneOffset");
                        return JavaTemplate.builder("#{any(java.time.Instant)}.atZone(ZoneOffset.UTC).format(#{any(java.time.format.DateTimeFormatter)})")
                                .imports("java.time.ZoneOffset").build()
                                .apply(getCursor(), m.getCoordinates().replace(), instant, m.getSelect());
                    }
                    if (isZonedDateTime(instant)) {
                        return JavaTemplate.apply("#{any(java.time.ZonedDateTime)}.format(#{any(java.time.format.DateTimeFormatter)})",
                                getCursor(), m.getCoordinates().replace(), instant, m.getSelect());
                    }
                    return m;
                }
                if (PRINT_READABLE_PARTIAL.matches(method) && javaType(m.getArguments().get(0)) != null) {
                    return JavaTemplate.apply("#{any(java.time.format.DateTimeFormatter)}.format(#{any(" + javaType(m.getArguments().get(0)) + ")})",
                            getCursor(), m.getCoordinates().replace(), m.getSelect(), m.getArguments().get(0));
                }
                if (WITH_ZONE_UTC.matches(method)) {
                    maybeAddImport("java.time.ZoneOffset");
                    return JavaTemplate
                            .builder("#{any(java.time.format.DateTimeFormatter)}.withZone(ZoneOffset.UTC)")
                            .imports("java.time.ZoneOffset").build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }

                if (TO_STRING_PATTERN.matches(method) || TO_STRING_PATTERN_LOCALE.matches(method)) {
                    String receiverType = javaType(m.getSelect());
                    String translated = translatedLiteral(m.getArguments().get(0));
                    if (receiverType == null || translated == null) {
                        return m;
                    }
                    boolean withLocale = m.getArguments().size() == 2;
                    String receiver = "java.time.Instant".equals(receiverType) ?
                            "#{any(java.time.Instant)}.atZone(ZoneOffset.UTC)" : "#{any(" + receiverType + ")}";
                    maybeAddImport("java.time.format.DateTimeFormatter");
                    maybeAddImport("java.time.ZoneOffset");
                    JavaTemplate template = JavaTemplate.builder(receiver + ".format(DateTimeFormatter.ofPattern(\"" + escape(translated) + "\"" +
                                    (withLocale ? ", #{any(java.util.Locale)}))" : "))"))
                            .imports("java.time.format.DateTimeFormatter", "java.time.ZoneOffset").build();
                    return withLocale ?
                            template.apply(getCursor(), m.getCoordinates().replace(), m.getSelect(), m.getArguments().get(1)) :
                            template.apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                if (PARTIAL_TO_STRING_FORMATTER.matches(method) && javaType(m.getSelect()) != null) {
                    return JavaTemplate.apply("#{any(" + javaType(m.getSelect()) + ")}.format(#{any(java.time.format.DateTimeFormatter)})",
                            getCursor(), m.getCoordinates().replace(), m.getSelect(), m.getArguments().get(0));
                }
                return m;
            }

            /// Joda-Time fills in the fields that the text does not have: the zone of the formatter or the default zone,
            /// and the start of the day. `java.time` only parses what is there, so when the pattern of the formatter is
            /// known to be date only, the day is started explicitly.
            private J parseZoned(J.MethodInvocation m, Expression text, @Nullable Expression formatter, String suffix) {
                if (formatter == null) {
                    return m;
                }
                String pattern = patternOf(formatter);
                boolean zoned = hasZone(formatter);
                maybeAddImport("java.time.ZonedDateTime");
                if (pattern != null && hasLetter(pattern, "yYu") && hasLetter(pattern, "ML") && hasLetter(pattern, "dD") &&
                    !hasLetter(pattern, "HhKkmsS") && (!zoned || JodaDateTimeToJavaTime.isSimple(formatter))) {
                    maybeAddImport("java.time.LocalDate");
                    maybeAddImport("java.time.ZoneId");
                    return JavaTemplate.builder("LocalDate.parse(#{any(java.lang.String)}, #{any(java.time.format.DateTimeFormatter)}).atStartOfDay(" +
                                    (zoned ? "#{any(java.time.format.DateTimeFormatter)}.getZone()" : "ZoneId.systemDefault()") + ")" + suffix)
                            .imports("java.time.LocalDate", "java.time.ZoneId").build()
                            .apply(getCursor(), m.getCoordinates().replace(), zoned ? new Object[]{text, formatter, formatter} : new Object[]{text, formatter});
                }
                if (zoned) {
                    return JavaTemplate.builder("ZonedDateTime.parse(#{any(java.lang.String)}, #{any(java.time.format.DateTimeFormatter)})" + suffix)
                            .imports("java.time.ZonedDateTime").build()
                            .apply(getCursor(), m.getCoordinates().replace(), text, formatter);
                }
                maybeAddImport("java.time.ZoneId");
                return JavaTemplate.builder("ZonedDateTime.parse(#{any(java.lang.String)}, #{any(java.time.format.DateTimeFormatter)}.withZone(ZoneId.systemDefault()))" + suffix)
                        .imports("java.time.ZonedDateTime", "java.time.ZoneId").build()
                        .apply(getCursor(), m.getCoordinates().replace(), text, formatter);
            }

            private @Nullable String patternOf(Expression formatter) {
                if (formatter instanceof J.MethodInvocation) {
                    J.MethodInvocation invocation = (J.MethodInvocation) formatter;
                    String name = invocation.getSimpleName();
                    if (("forPattern".equals(name) || "ofPattern".equals(name)) && invocation.getArguments().get(0) instanceof J.Literal) {
                        Object value = ((J.Literal) invocation.getArguments().get(0)).getValue();
                        return value instanceof String ? (String) value : null;
                    }
                    if (ISO.matches(invocation) && ISO_FORMATS.containsKey(name)) {
                        String code = ISO_FORMATS.get(name);
                        return code.contains("\"") ? code.substring(code.indexOf('"') + 1, code.lastIndexOf('"')) :
                                code.endsWith("ISO_LOCAL_DATE") ? "yyyy-MM-dd" : null;
                    }
                    if (("withZone".equals(name) || "withZoneUTC".equals(name) || "withLocale".equals(name)) && invocation.getSelect() != null) {
                        return patternOf(invocation.getSelect());
                    }
                    return null;
                }
                if (formatter instanceof J.FieldAccess && "ISO_LOCAL_DATE".equals(((J.FieldAccess) formatter).getSimpleName())) {
                    return "yyyy-MM-dd";
                }
                Expression initializer = initializer(formatter);
                return initializer == null ? null : patternOf(initializer);
            }

            private boolean hasZone(Expression formatter) {
                if (formatter instanceof J.MethodInvocation) {
                    J.MethodInvocation invocation = (J.MethodInvocation) formatter;
                    String name = invocation.getSimpleName();
                    return "withZone".equals(name) || "withZoneUTC".equals(name) ||
                            invocation.getSelect() != null && TypeUtils.isOfType(invocation.getType(), invocation.getSelect().getType()) &&
                                    hasZone(invocation.getSelect());
                }
                Expression initializer = initializer(formatter);
                return initializer != null && hasZone(initializer);
            }

            private @Nullable String translatedLiteral(Expression pattern) {
                if (pattern instanceof J.Literal && ((J.Literal) pattern).getValue() instanceof String) {
                    return translatePattern((String) ((J.Literal) pattern).getValue());
                }
                return null;
            }

            private @Nullable Expression initializer(Expression expression) {
                if (!(expression instanceof J.Identifier) || ((J.Identifier) expression).getFieldType() == null) {
                    return null;
                }
                JavaType.Variable variable = ((J.Identifier) expression).getFieldType();
                J.CompilationUnit cu = getCursor().firstEnclosing(J.CompilationUnit.class);
                if (cu == null) {
                    return null;
                }
                AtomicReference<@Nullable Expression> found = new AtomicReference<>();
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable v, Integer p) {
                        if (v.getVariableType() != null && v.getSimpleName().equals(variable.getName()) &&
                                String.valueOf(v.getVariableType().getOwner()).equals(String.valueOf(variable.getOwner()))) {
                            found.set(v.getInitializer());
                        }
                        return v;
                    }
                }.visit(cu, 0);
                return found.get();
            }
        });
    }

    private static boolean isZonedDateTime(Expression expression) {
        return JodaTimeTypes.isDateTime(expression.getType());
    }

    private static @Nullable String javaType(@Nullable Expression expression) {
        return expression == null ? null : JodaTimeTypes.javaTimeType(expression.getType());
    }

    private static boolean hasLetter(String pattern, String letters) {
        boolean quoted = false;
        for (char c : pattern.toCharArray()) {
            if (c == '\'') {
                quoted = !quoted;
            } else if (!quoted && letters.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }

    private static String escape(String pattern) {
        return pattern.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /// Translates a Joda-Time pattern to one that means the same to `java.time`, or returns `null` when it can not
    /// be expressed exactly, such as the week-based fields whose `java.time` letters depend on the locale.
    static @Nullable String translatePattern(String pattern) {
        StringBuilder translated = new StringBuilder();
        int i = 0;
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            if (c == '\'') {
                int end = i + 1;
                while (end < pattern.length() && pattern.charAt(end) != '\'') {
                    end++;
                }
                if (end >= pattern.length()) {
                    return null;
                }
                translated.append(pattern, i, end + 1);
                i = end + 1;
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                int end = i;
                while (end < pattern.length() && pattern.charAt(end) == c) {
                    end++;
                }
                String letters = translateLetters(c, end - i);
                if (letters == null) {
                    return null;
                }
                translated.append(letters);
                i = end;
            } else {
                // reserved for optional sections and future use in `java.time`, but literals in Joda-Time
                translated.append("[]{}#".indexOf(c) >= 0 ? "'" + c + "'" : String.valueOf(c));
                i++;
            }
        }
        return translated.toString();
    }

    private static @Nullable String translateLetters(char letter, int count) {
        switch (letter) {
            case 'Y':
                // year of era in Joda-Time, week-based year in `java.time`
                return repeat('y', count);
            case 'y':
            case 'S':
            case 'G':
                return repeat(letter, count);
            case 'M':
            case 'E':
            case 'z':
                return repeat(letter, Math.min(count, 4));
            case 'a':
                return "a";
            case 'd':
            case 'H':
            case 'h':
            case 'm':
            case 's':
            case 'K':
            case 'k':
                return count <= 2 ? repeat(letter, count) : null;
            case 'D':
                return count <= 3 ? repeat(letter, count) : null;
            case 'Z':
                return count == 1 ? "Z" : count == 2 ? "xxx" : "VV";
            default:
                return null;
        }
    }

    private static String repeat(char letter, int count) {
        StringBuilder repeated = new StringBuilder();
        for (int i = 0; i < count; i++) {
            repeated.append(letter);
        }
        return repeated.toString();
    }
}
