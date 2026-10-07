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
class JodaDateTimeZoneToJavaTimeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResource("/META-INF/rewrite/no-joda-time.yml", "org.openrewrite.java.joda.time.NoJodaTime")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "joda-time-2", "threeten-extra-1"));
    }

    @DocumentExample
    @Test
    void migrateDateTimeZone() {
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTimeZone;
              import java.util.TimeZone;

              class A {
                  public void foo() {
                      DateTimeZone.UTC.toString();
                      DateTimeZone.forID("America/New_York");
                      DateTimeZone.forOffsetHours(2);
                      DateTimeZone.forOffsetHoursMinutes(5, 30);
                      DateTimeZone.forTimeZone(TimeZone.getTimeZone("America/New_York"));
                  }
              }
              """,
            """
              import java.time.ZoneId;
              import java.time.ZoneOffset;
              import java.util.TimeZone;

              class A {
                  public void foo() {
                      ZoneOffset.UTC.toString();
                      ZoneId.of("America/New_York");
                      ZoneOffset.ofHours(2);
                      ZoneOffset.ofHoursMinutes(5, 30);
                      TimeZone.getTimeZone("America/New_York").toZoneId();
                  }
              }
              """
          )
        );
    }

    @Test
    void dateTimeZoneAccessors() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.DateTimeZone;

              import java.util.TimeZone;

              class A {
                  String id() {
                      return DateTimeZone.getDefault().getID();
                  }

                  TimeZone timeZone(DateTimeZone zone) {
                      return zone.toTimeZone();
                  }
              }
              """,
            """
              import java.time.ZoneId;
              import java.util.TimeZone;

              class A {
                  String id() {
                      return ZoneId.systemDefault().getId();
                  }

                  TimeZone timeZone(ZoneId zone) {
                      return TimeZone.getTimeZone(zone);
                  }
              }
              """
          )
        );
    }

    @Test
    void utcThroughStaticImportAndFullyQualified() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              import static org.joda.time.DateTimeZone.UTC;

              class A {
                  DateTime now() {
                      return DateTime.now(UTC);
                  }

                  Object utc() {
                      return org.joda.time.DateTimeZone.UTC;
                  }
              }
              """,
            """
              import java.time.ZoneOffset;
              import java.time.ZonedDateTime;

              import static java.time.ZoneOffset.UTC;

              class A {
                  ZonedDateTime now() {
                      return ZonedDateTime.now(UTC);
                  }

                  Object utc() {
                      return ZoneOffset.UTC;
                  }
              }
              """
          )
        );
    }
}
