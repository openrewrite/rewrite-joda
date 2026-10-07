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

/// The recipes run one after the other, so an expression may already have been migrated to `java.time` by the time
/// the next recipe looks at its type. These checks accept both the Joda-Time type and its `java.time` replacement.
final class JodaTimeTypes {

    private JodaTimeTypes() {
    }

    static boolean isDateTime(@Nullable JavaType type) {
        return TypeUtils.isOfClassType(type, "org.joda.time.DateTime") ||
                TypeUtils.isOfClassType(type, "org.joda.time.DateMidnight") ||
                TypeUtils.isOfClassType(type, "java.time.ZonedDateTime");
    }

    static boolean isInstant(@Nullable JavaType type) {
        return TypeUtils.isOfClassType(type, "org.joda.time.Instant") || TypeUtils.isOfClassType(type, "java.time.Instant");
    }

    static boolean isLocalDate(@Nullable JavaType type) {
        return TypeUtils.isOfClassType(type, "org.joda.time.LocalDate") || TypeUtils.isOfClassType(type, "java.time.LocalDate");
    }

    static boolean isLocalTime(@Nullable JavaType type) {
        return TypeUtils.isOfClassType(type, "org.joda.time.LocalTime") || TypeUtils.isOfClassType(type, "java.time.LocalTime");
    }
}
