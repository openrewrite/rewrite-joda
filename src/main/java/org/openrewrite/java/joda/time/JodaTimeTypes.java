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

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.HashMap;
import java.util.Map;

/// The recipes run one after the other, so an expression may already have been migrated to `java.time` by the time
/// the next recipe looks at its type. These checks accept both the Joda-Time type and its `java.time` replacement.
final class JodaTimeTypes {

    private static final Map<String, String> JAVA_TIME_TYPES = new HashMap<>();

    static {
        JAVA_TIME_TYPES.put("org.joda.time.DateTime", "java.time.ZonedDateTime");
        JAVA_TIME_TYPES.put("org.joda.time.DateMidnight", "java.time.ZonedDateTime");
        JAVA_TIME_TYPES.put("org.joda.time.Instant", "java.time.Instant");
        JAVA_TIME_TYPES.put("org.joda.time.LocalDate", "java.time.LocalDate");
        JAVA_TIME_TYPES.put("org.joda.time.LocalTime", "java.time.LocalTime");
        JAVA_TIME_TYPES.put("org.joda.time.LocalDateTime", "java.time.LocalDateTime");
        for (String javaTimeType : JAVA_TIME_TYPES.values().toArray(new String[0])) {
            JAVA_TIME_TYPES.put(javaTimeType, javaTimeType);
        }
    }

    private JodaTimeTypes() {
    }

    /// The `java.time` type that a Joda-Time date, time or instant becomes, or `null` for any other type.
    static @Nullable String javaTimeType(@Nullable JavaType type) {
        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
        return fq == null ? null : JAVA_TIME_TYPES.get(fq.getFullyQualifiedName());
    }

    static boolean isDateTime(@Nullable JavaType type) {
        return "java.time.ZonedDateTime".equals(javaTimeType(type));
    }

    static boolean isInstant(@Nullable JavaType type) {
        return "java.time.Instant".equals(javaTimeType(type));
    }

    static boolean isLocalDate(@Nullable JavaType type) {
        return "java.time.LocalDate".equals(javaTimeType(type));
    }

    static boolean isLocalTime(@Nullable JavaType type) {
        return "java.time.LocalTime".equals(javaTimeType(type));
    }
}
