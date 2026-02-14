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

import lombok.Getter;
import org.openrewrite.Recipe;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.ChangeType;

import java.util.ArrayList;
import java.util.List;

public class JodaTimeRecipe extends Recipe {
    @Getter
    final String displayName = "Migrate Joda-Time to Java time";

    @Getter
    final String description = "Prefer the Java standard library over third-party usage of Joda Time.";

    @Override
    public List<Recipe> getRecipeList() {
        List<Recipe> recipes = new ArrayList<>();

        // Phase 1: Rename methods (must run BEFORE ChangeType)
        // AbstractDateTime accessors
        recipes.add(new ChangeMethodName("org.joda.time.base.AbstractDateTime getHourOfDay()", "getHour", true, null));
        recipes.add(new ChangeMethodName("org.joda.time.base.AbstractDateTime getMinuteOfHour()", "getMinute", true, null));
        recipes.add(new ChangeMethodName("org.joda.time.base.AbstractDateTime getSecondOfMinute()", "getSecond", true, null));
        recipes.add(new ChangeMethodName("org.joda.time.base.AbstractDateTime getMonthOfYear()", "getMonthValue", true, null));
        // DateTime zone methods
        recipes.add(new ChangeMethodName("org.joda.time.DateTime withZone(org.joda.time.DateTimeZone)", "withZoneSameInstant", null, null));
        recipes.add(new ChangeMethodName("org.joda.time.DateTime withZoneRetainFields(org.joda.time.DateTimeZone)", "withZoneSameLocal", null, null));
        // DateTime setters
        recipes.add(new ChangeMethodName("org.joda.time.DateTime withMonthOfYear(int)", "withMonth", null, null));
        recipes.add(new ChangeMethodName("org.joda.time.DateTime withHourOfDay(int)", "withHour", null, null));
        recipes.add(new ChangeMethodName("org.joda.time.DateTime withMinuteOfHour(int)", "withMinute", null, null));
        recipes.add(new ChangeMethodName("org.joda.time.DateTime withSecondOfMinute(int)", "withSecond", null, null));
        // Duration static factories
        recipes.add(new ChangeMethodName("org.joda.time.Duration standardDays(long)", "ofDays", null, null));
        recipes.add(new ChangeMethodName("org.joda.time.Duration standardHours(long)", "ofHours", null, null));
        recipes.add(new ChangeMethodName("org.joda.time.Duration standardMinutes(long)", "ofMinutes", null, null));
        recipes.add(new ChangeMethodName("org.joda.time.Duration standardSeconds(long)", "ofSeconds", null, null));
        recipes.add(new ChangeMethodName("org.joda.time.Duration millis(long)", "ofMillis", null, null));
        // Duration instance getters
        recipes.add(new ChangeMethodName("org.joda.time.Duration getStandardDays()", "toDays", null, null));
        recipes.add(new ChangeMethodName("org.joda.time.Duration getStandardHours()", "toHours", null, null));
        recipes.add(new ChangeMethodName("org.joda.time.Duration getStandardMinutes()", "toMinutes", null, null));
        recipes.add(new ChangeMethodName("org.joda.time.Duration getStandardSeconds()", "getSeconds", null, null));
        // Duration conversion methods
        recipes.add(new ChangeMethodName("org.joda.time.Duration toStandardDays()", "toDays", null, null));
        recipes.add(new ChangeMethodName("org.joda.time.Duration toStandardHours()", "toHours", null, null));
        recipes.add(new ChangeMethodName("org.joda.time.Duration toStandardMinutes()", "toMinutes", null, null));
        recipes.add(new ChangeMethodName("org.joda.time.Duration toStandardSeconds()", "getSeconds", null, null));
        // DateTimeZone
        recipes.add(new ChangeMethodName("org.joda.time.DateTimeZone forID(String)", "of", null, null));
        // DateTimeFormat
        recipes.add(new ChangeMethodName("org.joda.time.format.DateTimeFormat forPattern(String)", "ofPattern", null, null));
        // Instant
        recipes.add(new ChangeMethodName("org.joda.time.Instant getMillis()", "toEpochMilli", null, null));
        // BaseDuration
        recipes.add(new ChangeMethodName("org.joda.time.base.BaseDuration getMillis()", "toMillis", true, null));

        // Phase 2: Imperative recipes for structural transformations
        recipes.add(new JodaDateTimeToJavaTime());
        recipes.add(new JodaAbstractInstantToJavaTime());
        recipes.add(new JodaDurationToJavaTime());
        recipes.add(new JodaIntervalToJavaTime());
        recipes.add(new JodaFormatterToJavaTime());
        recipes.add(new JodaDateTimeZoneToJavaTime());
        recipes.add(new JodaDateMidnightToJavaTime());
        recipes.add(new JodaInstantToJavaTime());

        // Phase 3: ChangeType (must run LAST)
        recipes.add(new ChangeType("org.joda.time.DateTime", "java.time.ZonedDateTime", null));
        recipes.add(new ChangeType("org.joda.time.base.BaseDateTime", "java.time.ZonedDateTime", null));
        recipes.add(new ChangeType("org.joda.time.base.AbstractDateTime", "java.time.ZonedDateTime", null));
        recipes.add(new ChangeType("org.joda.time.DateTimeZone", "java.time.ZoneId", null));
        recipes.add(new ChangeType("org.joda.time.format.DateTimeFormatter", "java.time.format.DateTimeFormatter", null));
        recipes.add(new ChangeType("org.joda.time.format.DateTimeFormat", "java.time.format.DateTimeFormatter", null));
        recipes.add(new ChangeType("org.joda.time.Duration", "java.time.Duration", null));
        recipes.add(new ChangeType("org.joda.time.ReadableDuration", "java.time.Duration", null));
        recipes.add(new ChangeType("org.joda.time.base.BaseDuration", "java.time.Duration", null));
        recipes.add(new ChangeType("org.joda.time.base.AbstractDuration", "java.time.Duration", null));
        recipes.add(new ChangeType("org.joda.time.Instant", "java.time.Instant", null));
        recipes.add(new ChangeType("org.joda.time.base.AbstractInstant", "java.time.Instant", null));
        recipes.add(new ChangeType("org.joda.time.ReadableInstant", "java.time.Instant", null));
        recipes.add(new ChangeType("org.joda.time.Interval", "org.threeten.extra.Interval", null));
        recipes.add(new ChangeType("org.joda.time.base.BaseInterval", "org.threeten.extra.Interval", null));
        recipes.add(new ChangeType("org.joda.time.base.AbstractInterval", "org.threeten.extra.Interval", null));
        recipes.add(new ChangeType("org.joda.time.DateMidnight", "java.time.ZonedDateTime", null));

        return recipes;
    }
}
