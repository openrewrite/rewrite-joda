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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@Execution(ExecutionMode.SAME_THREAD)
class JodaDateTimeToJavaTimeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResource("/META-INF/rewrite/no-joda-time.yml", "org.openrewrite.java.joda.time.NoJodaTime")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "joda-time-2", "threeten-extra-1"));
    }

    @DocumentExample
    @Test
    void migrateNewDateTime() {
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.DateTimeZone;
              import java.util.TimeZone;

              class A {
                  public void foo() {
                      new DateTime();
                      new DateTime(DateTimeZone.UTC);
                      new DateTime(1234567890L);
                      new DateTime(1234567890L, DateTimeZone.forID("America/New_York"));
                      new DateTime(2024, 9, 30, 12, 58);
                      new DateTime(2024, 9, 30, 12, 58, DateTimeZone.forOffsetHours(2));
                      new DateTime(2024, 9, 30, 13, 3, 15);
                      new DateTime(2024, 9, 30, 13, 3, 15, DateTimeZone.forOffsetHoursMinutes(5, 30));
                      new DateTime(2024, 9, 30, 13, 49, 15, 545);
                      new DateTime(2024, 9, 30, 13, 49, 15, 545, DateTimeZone.forTimeZone(TimeZone.getTimeZone("America/New_York")));
                  }
              }
              """,
            """
              import java.time.Instant;
              import java.time.ZoneId;
              import java.time.ZoneOffset;
              import java.time.ZonedDateTime;
              import java.util.TimeZone;

              class A {
                  public void foo() {
                      ZonedDateTime.now();
                      ZonedDateTime.now(ZoneOffset.UTC);
                      ZonedDateTime.ofInstant(Instant.ofEpochMilli(1234567890L), ZoneId.systemDefault());
                      ZonedDateTime.ofInstant(Instant.ofEpochMilli(1234567890L), ZoneId.of("America/New_York"));
                      ZonedDateTime.of(2024, 9, 30, 12, 58, 0, 0, ZoneId.systemDefault());
                      ZonedDateTime.of(2024, 9, 30, 12, 58, 0, 0, ZoneOffset.ofHours(2));
                      ZonedDateTime.of(2024, 9, 30, 13, 3, 15, 0, ZoneId.systemDefault());
                      ZonedDateTime.of(2024, 9, 30, 13, 3, 15, 0, ZoneOffset.ofHoursMinutes(5, 30));
                      ZonedDateTime.of(2024, 9, 30, 13, 49, 15, 545 * 1_000_000, ZoneId.systemDefault());
                      ZonedDateTime.of(2024, 9, 30, 13, 49, 15, 545 * 1_000_000, TimeZone.getTimeZone("America/New_York").toZoneId());
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateDateTimeStaticCalls() {
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.DateTimeZone;
              import org.joda.time.format.DateTimeFormat;
              import java.util.TimeZone;

              class A {
                  public void foo() {
                      DateTime.now();
                      DateTime.now(DateTimeZone.forTimeZone(TimeZone.getTimeZone("America/New_York")));
                      DateTime.parse("2024-09-30T23:03:00.000Z");
                      DateTime.parse("2024-09-30T23:03:00.000Z", DateTimeFormat.shortDate());
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;
              import java.time.format.DateTimeFormatter;
              import java.time.format.FormatStyle;
              import java.util.TimeZone;

              class A {
                  public void foo() {
                      ZonedDateTime.now();
                      ZonedDateTime.now(TimeZone.getTimeZone("America/New_York").toZoneId());
                      ZonedDateTime.parse("2024-09-30T23:03:00.000Z");
                      ZonedDateTime.parse("2024-09-30T23:03:00.000Z", DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateDateTimeInstanceCalls() {
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.DateTimeZone;
              import org.joda.time.Duration;
              import java.util.TimeZone;

              class A {
                  public void foo() {
                      new DateTime().toDateTime();
                      new DateTime().toDateTime(DateTimeZone.forTimeZone(TimeZone.getTimeZone("America/New_York")));
                      new DateTime().withMillis(1234567890L);
                      new DateTime().withZone(DateTimeZone.forID("America/New_York"));
                      new DateTime().withZoneRetainFields(DateTimeZone.forID("America/New_York"));
                      new DateTime().withEarlierOffsetAtOverlap();
                      new DateTime().withLaterOffsetAtOverlap();
                      new DateTime().withDate(2024, 9, 30);
                      new DateTime().withTime(12, 58, 57, 550);
                      new DateTime().withDurationAdded(1234567890L, 2);
                      new DateTime().plus(1234567890L);
                      new DateTime().plus(Duration.standardDays(1));
                      new DateTime().plusYears(1);
                      new DateTime().plusMonths(1);
                      new DateTime().plusWeeks(1);
                      new DateTime().plusDays(1);
                      new DateTime().plusHours(1);
                      new DateTime().plusMinutes(1);
                      new DateTime().plusSeconds(1);
                      new DateTime().plusMillis(1);
                      new DateTime().minus(1234567890L);
                      new DateTime().minus(Duration.standardDays(1));
                      new DateTime().minusYears(1);
                      new DateTime().minusMonths(1);
                      new DateTime().minusWeeks(1);
                      new DateTime().minusDays(1);
                      new DateTime().minusHours(1);
                      new DateTime().minusMinutes(1);
                      new DateTime().minusSeconds(1);
                      new DateTime().minusMillis(1);
                      new DateTime().toLocalDateTime();
                      new DateTime().toLocalDate();
                      new DateTime().toLocalTime();
                      new DateTime().withYear(2024);
                      new DateTime().withWeekyear(2024);
                      new DateTime().withMonthOfYear(9);
                      new DateTime().withWeekOfWeekyear(39);
                      new DateTime().withDayOfYear(273);
                      new DateTime().withDayOfMonth(30);
                      new DateTime().withDayOfWeek(1);
                      new DateTime().withHourOfDay(12);
                      new DateTime().withMinuteOfHour(58);
                      new DateTime().withSecondOfMinute(57);
                      new DateTime().withMillisOfSecond(550);
                      new DateTime().withMillisOfDay(123456);
                      new DateTime().withTimeAtStartOfDay();
                  }
              }
              """,
            """
              import java.time.Duration;
              import java.time.Instant;
              import java.time.ZoneId;
              import java.time.ZonedDateTime;
              import java.time.temporal.ChronoField;
              import java.time.temporal.IsoFields;
              import java.util.TimeZone;

              class A {
                  public void foo() {
                      ZonedDateTime.now();
                      ZonedDateTime.now().withZoneSameInstant(TimeZone.getTimeZone("America/New_York").toZoneId());
                      ZonedDateTime.ofInstant(Instant.ofEpochMilli(1234567890L), ZonedDateTime.now().getZone());
                      ZonedDateTime.now().withZoneSameInstant(ZoneId.of("America/New_York"));
                      ZonedDateTime.now().withZoneSameLocal(ZoneId.of("America/New_York"));
                      ZonedDateTime.now().withEarlierOffsetAtOverlap();
                      ZonedDateTime.now().withLaterOffsetAtOverlap();
                      ZonedDateTime.now().withYear(2024).withMonth(9).withDayOfMonth(30);
                      ZonedDateTime.now().withHour(12).withMinute(58).withSecond(57).withNano(550 * 1_000_000);
                      ZonedDateTime.now().plus(Duration.ofMillis(1234567890L).multipliedBy(2));
                      ZonedDateTime.now().plus(Duration.ofMillis(1234567890L));
                      ZonedDateTime.now().plus(Duration.ofDays(1));
                      ZonedDateTime.now().plusYears(1);
                      ZonedDateTime.now().plusMonths(1);
                      ZonedDateTime.now().plusWeeks(1);
                      ZonedDateTime.now().plusDays(1);
                      ZonedDateTime.now().plusHours(1);
                      ZonedDateTime.now().plusMinutes(1);
                      ZonedDateTime.now().plusSeconds(1);
                      ZonedDateTime.now().plus(Duration.ofMillis(1));
                      ZonedDateTime.now().minus(Duration.ofMillis(1234567890L));
                      ZonedDateTime.now().minus(Duration.ofDays(1));
                      ZonedDateTime.now().minusYears(1);
                      ZonedDateTime.now().minusMonths(1);
                      ZonedDateTime.now().minusWeeks(1);
                      ZonedDateTime.now().minusDays(1);
                      ZonedDateTime.now().minusHours(1);
                      ZonedDateTime.now().minusMinutes(1);
                      ZonedDateTime.now().minusSeconds(1);
                      ZonedDateTime.now().minus(Duration.ofMillis(1));
                      ZonedDateTime.now().toLocalDateTime();
                      ZonedDateTime.now().toLocalDate();
                      ZonedDateTime.now().toLocalTime();
                      ZonedDateTime.now().withYear(2024);
                      ZonedDateTime.now().with(IsoFields.WEEK_BASED_YEAR, 2024);
                      ZonedDateTime.now().withMonth(9);
                      ZonedDateTime.now().with(ChronoField.ALIGNED_WEEK_OF_YEAR, 39);
                      ZonedDateTime.now().withDayOfYear(273);
                      ZonedDateTime.now().withDayOfMonth(30);
                      ZonedDateTime.now().with(ChronoField.DAY_OF_WEEK, 1);
                      ZonedDateTime.now().withHour(12);
                      ZonedDateTime.now().withMinute(58);
                      ZonedDateTime.now().withSecond(57);
                      ZonedDateTime.now().withNano(550 * 1_000_000);
                      ZonedDateTime.now().with(ChronoField.MILLI_OF_DAY, 123456);
                      ZonedDateTime.now().toLocalDate().atStartOfDay(ZonedDateTime.now().getZone());
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateAbstractDateTime() {
        // language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;

              class A {
                  public void foo() {
                      new DateTime().getDayOfMonth();
                      new DateTime().getDayOfWeek();
                      new DateTime().getHourOfDay();
                      new DateTime().getMillisOfSecond();
                      new DateTime().getMinuteOfDay();
                      new DateTime().getMinuteOfHour();
                      new DateTime().getMonthOfYear();
                      new DateTime().getSecondOfDay();
                      new DateTime().getSecondOfMinute();
                      new DateTime().getWeekOfWeekyear();
                      new DateTime().toString();
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;
              import java.time.temporal.ChronoField;

              class A {
                  public void foo() {
                      ZonedDateTime.now().getDayOfMonth();
                      ZonedDateTime.now().getDayOfWeek().getValue();
                      ZonedDateTime.now().getHour();
                      ZonedDateTime.now().get(ChronoField.MILLI_OF_SECOND);
                      ZonedDateTime.now().get(ChronoField.MINUTE_OF_DAY);
                      ZonedDateTime.now().getMinute();
                      ZonedDateTime.now().getMonthValue();
                      ZonedDateTime.now().get(ChronoField.SECOND_OF_DAY);
                      ZonedDateTime.now().getSecond();
                      ZonedDateTime.now().get(ChronoField.ALIGNED_WEEK_OF_YEAR);
                      ZonedDateTime.now().toString();
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateJodaTypeExpressionReferencingNonJodaTypeVar() {
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;

              class A {
                  public void foo() {
                      long millis = DateTime.now().getMillis();
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;

              class A {
                  public void foo() {
                      long millis = ZonedDateTime.now().toInstant().toEpochMilli();
                  }
              }
              """
          )
        );
    }
}
