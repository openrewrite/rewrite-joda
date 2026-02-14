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
class JodaLocalTimeToJavaTimeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResource("/META-INF/rewrite/no-joda-time.yml", "org.openrewrite.java.joda.time.NoJodaTime")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "joda-time-2", "threeten-extra-1"));
    }

    @DocumentExample
    @Test
    void migrateLocalTime() {
        // language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTimeZone;
              import org.joda.time.LocalTime;

              class A {
                  public void foo() {
                      new LocalTime();
                      new LocalTime(DateTimeZone.UTC);
                      new LocalTime(10, 30);
                      new LocalTime(10, 30, 45);
                      new LocalTime(10, 30, 45, 500);
                      LocalTime.now().plusMillis(100);
                      LocalTime.now().minusMillis(100);
                      LocalTime.now().withMillisOfSecond(500);
                      LocalTime.now().getMillisOfSecond();
                      LocalTime.now().getMillisOfDay();
                      LocalTime.now().getHourOfDay();
                      LocalTime.now().getMinuteOfHour();
                      LocalTime.now().getSecondOfMinute();
                      LocalTime.now().withHourOfDay(10);
                      LocalTime.now().withMinuteOfHour(30);
                      LocalTime.now().withSecondOfMinute(45);
                      LocalTime.now().toDateTimeToday();
                      LocalTime.now().toDateTimeToday(DateTimeZone.UTC);
                  }
              }
              """,
            """
              import java.time.LocalDate;
              import java.time.LocalTime;
              import java.time.ZoneId;
              import java.time.ZoneOffset;
              import java.time.temporal.ChronoField;

              class A {
                  public void foo() {
                      LocalTime.now();
                      LocalTime.now(ZoneOffset.UTC);
                      LocalTime.of(10, 30);
                      LocalTime.of(10, 30, 45);
                      LocalTime.of(10, 30, 45, 500 * 1_000_000);
                      LocalTime.now().plusNanos(100 * 1_000_000L);
                      LocalTime.now().minusNanos(100 * 1_000_000L);
                      LocalTime.now().withNano(500 * 1_000_000);
                      LocalTime.now().get(ChronoField.MILLI_OF_SECOND);
                      LocalTime.now().get(ChronoField.MILLI_OF_DAY);
                      LocalTime.now().getHour();
                      LocalTime.now().getMinute();
                      LocalTime.now().getSecond();
                      LocalTime.now().withHour(10);
                      LocalTime.now().withMinute(30);
                      LocalTime.now().withSecond(45);
                      LocalTime.now().atDate(LocalDate.now()).atZone(ZoneId.systemDefault());
                      LocalTime.now().atDate(LocalDate.now(ZoneOffset.UTC)).atZone(ZoneOffset.UTC);
                  }
              }
              """
          )
        );
    }
}
