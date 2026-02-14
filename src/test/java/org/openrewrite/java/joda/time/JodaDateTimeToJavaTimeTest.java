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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@Execution(ExecutionMode.SAME_THREAD)
class JodaDateTimeToJavaTimeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResource("/META-INF/rewrite/joda-time.yml", "org.openrewrite.java.joda.time.JodaTimeRecipe")
          .parser(JavaParser.fromJavaVersion().classpath("joda-time", "threeten-extra"));
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
                      System.out.println(new DateTime());
                      System.out.println(new DateTime(DateTimeZone.UTC));
                      System.out.println(new DateTime(1234567890L));
                      System.out.println(new DateTime(1234567890L, DateTimeZone.forID("America/New_York")));
                      System.out.println(new DateTime(2024, 9, 30, 12, 58));
                      System.out.println(new DateTime(2024, 9, 30, 12, 58, DateTimeZone.forOffsetHours(2)));
                      System.out.println(new DateTime(2024, 9, 30, 13, 3, 15));
                      System.out.println(new DateTime(2024, 9, 30, 13, 3, 15, DateTimeZone.forOffsetHoursMinutes(5, 30)));
                      System.out.println(new DateTime(2024, 9, 30, 13, 49, 15, 545));
                      System.out.println(new DateTime(2024, 9, 30, 13, 49, 15, 545, DateTimeZone.forTimeZone(TimeZone.getTimeZone("America/New_York"))));
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
                      System.out.println(ZonedDateTime.now());
                      System.out.println(ZonedDateTime.now(ZoneOffset.UTC));
                      System.out.println(ZonedDateTime.ofInstant(Instant.ofEpochMilli(1234567890L), ZoneId.systemDefault()));
                      System.out.println(ZonedDateTime.ofInstant(Instant.ofEpochMilli(1234567890L), ZoneId.of("America/New_York")));
                      System.out.println(ZonedDateTime.of(2024, 9, 30, 12, 58, 0, 0, ZoneId.systemDefault()));
                      System.out.println(ZonedDateTime.of(2024, 9, 30, 12, 58, 0, 0, ZoneOffset.ofHours(2)));
                      System.out.println(ZonedDateTime.of(2024, 9, 30, 13, 3, 15, 0, ZoneId.systemDefault()));
                      System.out.println(ZonedDateTime.of(2024, 9, 30, 13, 3, 15, 0, ZoneOffset.ofHoursMinutes(5, 30)));
                      System.out.println(ZonedDateTime.of(2024, 9, 30, 13, 49, 15, 545 * 1_000_000, ZoneId.systemDefault()));
                      System.out.println(ZonedDateTime.of(2024, 9, 30, 13, 49, 15, 545 * 1_000_000, TimeZone.getTimeZone("America/New_York").toZoneId()));
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
                      System.out.println(DateTime.now());
                      System.out.println(DateTime.now(DateTimeZone.forTimeZone(TimeZone.getTimeZone("America/New_York"))));
                      System.out.println(DateTime.parse("2024-09-30T23:03:00.000Z"));
                      System.out.println(DateTime.parse("2024-09-30T23:03:00.000Z", DateTimeFormat.shortDate()));
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
                      System.out.println(ZonedDateTime.now());
                      System.out.println(ZonedDateTime.now(TimeZone.getTimeZone("America/New_York").toZoneId()));
                      System.out.println(ZonedDateTime.parse("2024-09-30T23:03:00.000Z"));
                      System.out.println(ZonedDateTime.parse("2024-09-30T23:03:00.000Z", DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)));
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
                      System.out.println(new DateTime().toDateTime());
                      System.out.println(new DateTime().toDateTime(DateTimeZone.forTimeZone(TimeZone.getTimeZone("America/New_York"))));
                      System.out.println(new DateTime().withMillis(1234567890L));
                      System.out.println(new DateTime().withZone(DateTimeZone.forID("America/New_York")));
                      System.out.println(new DateTime().withZoneRetainFields(DateTimeZone.forID("America/New_York")));
                      System.out.println(new DateTime().withEarlierOffsetAtOverlap());
                      System.out.println(new DateTime().withLaterOffsetAtOverlap());
                      System.out.println(new DateTime().withDate(2024, 9, 30));
                      System.out.println(new DateTime().withTime(12, 58, 57, 550));
                      System.out.println(new DateTime().withDurationAdded(1234567890L, 2));
                      System.out.println(new DateTime().plus(1234567890L));
                      System.out.println(new DateTime().plus(Duration.standardDays(1)));
                      System.out.println(new DateTime().plusYears(1));
                      System.out.println(new DateTime().plusMonths(1));
                      System.out.println(new DateTime().plusWeeks(1));
                      System.out.println(new DateTime().plusDays(1));
                      System.out.println(new DateTime().plusHours(1));
                      System.out.println(new DateTime().plusMinutes(1));
                      System.out.println(new DateTime().plusSeconds(1));
                      System.out.println(new DateTime().plusMillis(1));
                      System.out.println(new DateTime().minus(1234567890L));
                      System.out.println(new DateTime().minus(Duration.standardDays(1)));
                      System.out.println(new DateTime().minusYears(1));
                      System.out.println(new DateTime().minusMonths(1));
                      System.out.println(new DateTime().minusWeeks(1));
                      System.out.println(new DateTime().minusDays(1));
                      System.out.println(new DateTime().minusHours(1));
                      System.out.println(new DateTime().minusMinutes(1));
                      System.out.println(new DateTime().minusSeconds(1));
                      System.out.println(new DateTime().minusMillis(1));
                      System.out.println(new DateTime().toLocalDateTime());
                      System.out.println(new DateTime().toLocalDate());
                      System.out.println(new DateTime().toLocalTime());
                      System.out.println(new DateTime().withYear(2024));
                      System.out.println(new DateTime().withWeekyear(2024));
                      System.out.println(new DateTime().withMonthOfYear(9));
                      System.out.println(new DateTime().withWeekOfWeekyear(39));
                      System.out.println(new DateTime().withDayOfYear(273));
                      System.out.println(new DateTime().withDayOfMonth(30));
                      System.out.println(new DateTime().withDayOfWeek(1));
                      System.out.println(new DateTime().withHourOfDay(12));
                      System.out.println(new DateTime().withMinuteOfHour(58));
                      System.out.println(new DateTime().withSecondOfMinute(57));
                      System.out.println(new DateTime().withMillisOfSecond(550));
                      System.out.println(new DateTime().withMillisOfDay(123456));
                      System.out.println(new DateTime().withTimeAtStartOfDay());
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
                      System.out.println(ZonedDateTime.now());
                      System.out.println(ZonedDateTime.now().withZoneSameInstant(TimeZone.getTimeZone("America/New_York").toZoneId()));
                      System.out.println(ZonedDateTime.ofInstant(Instant.ofEpochMilli(1234567890L), ZonedDateTime.now().getZone()));
                      System.out.println(ZonedDateTime.now().withZoneSameInstant(ZoneId.of("America/New_York")));
                      System.out.println(ZonedDateTime.now().withZoneSameLocal(ZoneId.of("America/New_York")));
                      System.out.println(ZonedDateTime.now().withEarlierOffsetAtOverlap());
                      System.out.println(ZonedDateTime.now().withLaterOffsetAtOverlap());
                      System.out.println(ZonedDateTime.now().withYear(2024).withMonth(9).withDayOfMonth(30));
                      System.out.println(ZonedDateTime.now().withHour(12).withMinute(58).withSecond(57).withNano(550 * 1_000_000));
                      System.out.println(ZonedDateTime.now().plus(Duration.ofMillis(1234567890L).multipliedBy(2)));
                      System.out.println(ZonedDateTime.now().plus(Duration.ofMillis(1234567890L)));
                      System.out.println(ZonedDateTime.now().plus(Duration.ofDays(1)));
                      System.out.println(ZonedDateTime.now().plusYears(1));
                      System.out.println(ZonedDateTime.now().plusMonths(1));
                      System.out.println(ZonedDateTime.now().plusWeeks(1));
                      System.out.println(ZonedDateTime.now().plusDays(1));
                      System.out.println(ZonedDateTime.now().plusHours(1));
                      System.out.println(ZonedDateTime.now().plusMinutes(1));
                      System.out.println(ZonedDateTime.now().plusSeconds(1));
                      System.out.println(ZonedDateTime.now().plus(Duration.ofMillis(1)));
                      System.out.println(ZonedDateTime.now().minus(Duration.ofMillis(1234567890L)));
                      System.out.println(ZonedDateTime.now().minus(Duration.ofDays(1)));
                      System.out.println(ZonedDateTime.now().minusYears(1));
                      System.out.println(ZonedDateTime.now().minusMonths(1));
                      System.out.println(ZonedDateTime.now().minusWeeks(1));
                      System.out.println(ZonedDateTime.now().minusDays(1));
                      System.out.println(ZonedDateTime.now().minusHours(1));
                      System.out.println(ZonedDateTime.now().minusMinutes(1));
                      System.out.println(ZonedDateTime.now().minusSeconds(1));
                      System.out.println(ZonedDateTime.now().minus(Duration.ofMillis(1)));
                      System.out.println(ZonedDateTime.now().toLocalDateTime());
                      System.out.println(ZonedDateTime.now().toLocalDate());
                      System.out.println(ZonedDateTime.now().toLocalTime());
                      System.out.println(ZonedDateTime.now().withYear(2024));
                      System.out.println(ZonedDateTime.now().with(IsoFields.WEEK_BASED_YEAR, 2024));
                      System.out.println(ZonedDateTime.now().withMonth(9));
                      System.out.println(ZonedDateTime.now().with(ChronoField.ALIGNED_WEEK_OF_YEAR, 39));
                      System.out.println(ZonedDateTime.now().withDayOfYear(273));
                      System.out.println(ZonedDateTime.now().withDayOfMonth(30));
                      System.out.println(ZonedDateTime.now().with(ChronoField.DAY_OF_WEEK, 1));
                      System.out.println(ZonedDateTime.now().withHour(12));
                      System.out.println(ZonedDateTime.now().withMinute(58));
                      System.out.println(ZonedDateTime.now().withSecond(57));
                      System.out.println(ZonedDateTime.now().withNano(550 * 1_000_000));
                      System.out.println(ZonedDateTime.now().with(ChronoField.MILLI_OF_DAY, 123456));
                      System.out.println(ZonedDateTime.now().toLocalDate().atStartOfDay(ZonedDateTime.now().getZone()));
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
                      System.out.println(millis);
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;

              class A {
                  public void foo() {
                      long millis = ZonedDateTime.now().toInstant().toEpochMilli();
                      System.out.println(millis);
                  }
              }
              """
          )
        );
    }
}
