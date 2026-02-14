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
class JodaTimePeriodToJavaTimeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResource("/META-INF/rewrite/no-joda-time.yml", "org.openrewrite.java.joda.time.NoJodaTime")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "joda-time-2", "threeten-extra-1"));
    }

    @DocumentExample
    @Test
    void daysBetweenGetDays() {
        // language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.Days;

              class A {
                  void foo(DateTime start, DateTime end) {
                      int days = Days.daysBetween(start, end).getDays();
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;
              import java.time.temporal.ChronoUnit;

              class A {
                  void foo(ZonedDateTime start, ZonedDateTime end) {
                      int days = (int) ChronoUnit.DAYS.between(start, end);
                  }
              }
              """
          )
        );
    }

    @Test
    void hoursBetweenGetHours() {
        // language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.Hours;

              class A {
                  void foo(DateTime start, DateTime end) {
                      int hours = Hours.hoursBetween(start, end).getHours();
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;
              import java.time.temporal.ChronoUnit;

              class A {
                  void foo(ZonedDateTime start, ZonedDateTime end) {
                      int hours = (int) ChronoUnit.HOURS.between(start, end);
                  }
              }
              """
          )
        );
    }

    @Test
    void minutesBetweenGetMinutes() {
        // language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.Minutes;

              class A {
                  void foo(DateTime start, DateTime end) {
                      int minutes = Minutes.minutesBetween(start, end).getMinutes();
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;
              import java.time.temporal.ChronoUnit;

              class A {
                  void foo(ZonedDateTime start, ZonedDateTime end) {
                      int minutes = (int) ChronoUnit.MINUTES.between(start, end);
                  }
              }
              """
          )
        );
    }

    @Test
    void secondsBetweenGetSeconds() {
        // language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.Seconds;

              class A {
                  void foo(DateTime start, DateTime end) {
                      int seconds = Seconds.secondsBetween(start, end).getSeconds();
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;
              import java.time.temporal.ChronoUnit;

              class A {
                  void foo(ZonedDateTime start, ZonedDateTime end) {
                      int seconds = (int) ChronoUnit.SECONDS.between(start, end);
                  }
              }
              """
          )
        );
    }

    @Test
    void standaloneFactoryCall() {
        // language=java
        rewriteRun(
          java(
            """
              import org.joda.time.Seconds;

              class A {
                  void foo(int n) {
                      Object d = Seconds.seconds(n);
                  }
              }
              """,
            """
              import java.time.Duration;

              class A {
                  void foo(int n) {
                      Object d = Duration.ofSeconds(n);
                  }
              }
              """
          )
        );
    }

    @Test
    void daysFactoryToStandardDuration() {
        // language=java
        rewriteRun(
          java(
            """
              import org.joda.time.Days;
              import org.joda.time.Duration;

              class A {
                  void foo(int n) {
                      Duration d = Days.days(n).toStandardDuration();
                  }
              }
              """,
            """
              import java.time.Duration;

              class A {
                  void foo(int n) {
                      Duration d = Duration.ofDays(n);
                  }
              }
              """
          )
        );
    }

    @Test
    void hoursFactoryToStandardDuration() {
        // language=java
        rewriteRun(
          java(
            """
              import org.joda.time.Duration;
              import org.joda.time.Hours;

              class A {
                  void foo(int n) {
                      Duration d = Hours.hours(n).toStandardDuration();
                  }
              }
              """,
            """
              import java.time.Duration;

              class A {
                  void foo(int n) {
                      Duration d = Duration.ofHours(n);
                  }
              }
              """
          )
        );
    }

    @Test
    void minutesFactoryToStandardDuration() {
        // language=java
        rewriteRun(
          java(
            """
              import org.joda.time.Duration;
              import org.joda.time.Minutes;

              class A {
                  void foo(int n) {
                      Duration d = Minutes.minutes(n).toStandardDuration();
                  }
              }
              """,
            """
              import java.time.Duration;

              class A {
                  void foo(int n) {
                      Duration d = Duration.ofMinutes(n);
                  }
              }
              """
          )
        );
    }

    @Test
    void secondsFactoryToStandardDuration() {
        // language=java
        rewriteRun(
          java(
            """
              import org.joda.time.Duration;
              import org.joda.time.Seconds;

              class A {
                  void foo(int n) {
                      Duration d = Seconds.seconds(n).toStandardDuration();
                  }
              }
              """,
            """
              import java.time.Duration;

              class A {
                  void foo(int n) {
                      Duration d = Duration.ofSeconds(n);
                  }
              }
              """
          )
        );
    }

    @Test
    void daysBetweenToStandardDuration() {
        // language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.Days;
              import org.joda.time.Duration;

              class A {
                  void foo(DateTime start, DateTime end) {
                      Duration d = Days.daysBetween(start, end).toStandardDuration();
                  }
              }
              """,
            """
              import java.time.Duration;
              import java.time.ZonedDateTime;
              import java.time.temporal.ChronoUnit;

              class A {
                  void foo(ZonedDateTime start, ZonedDateTime end) {
                      Duration d = Duration.ofDays(ChronoUnit.DAYS.between(start, end));
                  }
              }
              """
          )
        );
    }
}
