/*
 * Copyright 2025 the original author or authors.
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

import lombok.Getter;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.joda.time.templates.TimeClassNames.*;

public class DateTimeMidnightTemplates implements Templates {
    private final MethodMatcher newDateTimeMidnight = new MethodMatcher(JODA_DATE_TIME_MIDNIGHT + " <constructor>()");
    private final MethodMatcher dateTimeMidnightNow = new MethodMatcher(JODA_DATE_TIME_MIDNIGHT + " now()");

    private final JavaTemplate dateTimeAtStartOfDayTemplate = JavaTemplate
            .builder("LocalDate.now().atStartOfDay(ZoneOffset.of(ZoneId.systemDefault().getId()))")
            .imports(JAVA_LOCAL_DATE, JAVA_LOCAL_DATE_TIME, JAVA_ZONE_OFFSET, JAVA_ZONE_ID)
            .build();

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {{
                add(new MethodTemplate(newDateTimeMidnight, dateTimeAtStartOfDayTemplate));
                add(new MethodTemplate(dateTimeMidnightNow, dateTimeAtStartOfDayTemplate));
            }};
}
