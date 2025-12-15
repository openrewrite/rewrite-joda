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
package org.openrewrite.java.joda.time.templates;

import org.openrewrite.java.joda.time.MappingLogger;
import org.openrewrite.java.tree.JavaType;

import java.util.HashMap;
import java.util.Map;

import static org.openrewrite.java.joda.time.templates.TimeClassNames.*;

public class TimeClassMap {

    private final MappingLogger logger;

    public TimeClassMap(MappingLogger logger) {
        this.logger = logger;
    }

    private final Map<String, JavaType.Class> jodaToJavaTimeMap = new HashMap<String, JavaType.Class>() {
        {
            put(JODA_DATE_TIME, javaTypeClass(JAVA_DATE_TIME));
            put(JODA_BASE_DATE_TIME, javaTypeClass(JAVA_DATE_TIME));
            put(JODA_DATE_TIME_ZONE, javaTypeClass(JAVA_ZONE_ID));
            put(JODA_TIME_FORMATTER, javaTypeClass(JAVA_TIME_FORMATTER));
            put(JODA_DURATION, javaTypeClass(JAVA_DURATION));
            put(JODA_READABLE_DURATION, javaTypeClass(JAVA_DURATION));
            put(JODA_INTERVAL, javaTypeClass(THREE_TEN_EXTRA_INTERVAL));
        }
    };

    private final Map<String, String> jodaToJavaTimeShortName = new HashMap<String, String>() {
        {
            put(JODA_DATE_TIME, "ZonedDateTime");
            put(JODA_DATE_TIME_ZONE, "ZoneId");
        }
    };

    private JavaType.Class javaTypeClass(String fqn) {
        return (JavaType.Class)JavaType.buildType(fqn);
    }

    public JavaType getJavaTimeType(JavaType type) {
        if (!(type instanceof JavaType.Class)) {
            logger.info("Not a JavaType.Class", type);
            return null;
        }

        JavaType.Class clazz = (JavaType.Class) type;
        String fullyQualifiedName = clazz.getFullyQualifiedName();
        JavaType.Class javaClass = jodaToJavaTimeMap.get(fullyQualifiedName);
        if (javaClass == null) {
            logger.info("Joda type is found but mapping is missing", clazz);
            return null;
        }

        return javaClass;
    }

    public String getJavaTimeShortName(JavaType type) {
        if (!(type instanceof JavaType.Class)) {
            logger.info("Not a JavaType.Class: ", type);
            return null;
        }

        JavaType.Class clazz = (JavaType.Class) type;
        String fullyQualifiedName = clazz.getFullyQualifiedName();
        String result = jodaToJavaTimeShortName.get(fullyQualifiedName);
        if(result == null) {
            logger.info("Joda type is found but mapping is missing", type);
            String[] split = fullyQualifiedName.split("\\.");
            return split[split.length - 1];
        }

        return result;
    }
}
