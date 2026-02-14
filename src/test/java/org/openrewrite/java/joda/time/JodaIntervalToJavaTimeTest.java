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
class JodaIntervalToJavaTimeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResource("/META-INF/rewrite/no-joda-time.yml", "org.openrewrite.java.joda.time.NoJodaTime")
          .parser(JavaParser.fromJavaVersion().classpath("joda-time", "threeten-extra"));
    }

    @DocumentExample
    @Test
    void migrateInterval() {
        // language=java
        rewriteRun(
          java(
            """
                import org.joda.time.DateTime;
                import org.joda.time.Duration;
                import org.joda.time.Interval;
                import org.joda.time.DateTimeZone;

                class A {
                    public void foo() {
                        System.out.println(new Interval(50, 100));
                        System.out.println(new Interval(50, 100, DateTimeZone.UTC));
                        System.out.println(new Interval(DateTime.now(), DateTime.now().plusDays(1)));
                        System.out.println(new Interval(DateTime.now(), Duration.standardDays(1)));
                    }
                }
                """,
            """
              import org.threeten.extra.Interval;

              import java.time.Duration;
              import java.time.Instant;
              import java.time.ZonedDateTime;

              class A {
                  public void foo() {
                      System.out.println(Interval.of(Instant.ofEpochMilli(50), Instant.ofEpochMilli(100)));
                      System.out.println(Interval.of(Instant.ofEpochMilli(50), Instant.ofEpochMilli(100)));
                      System.out.println(Interval.of(ZonedDateTime.now().toInstant(), ZonedDateTime.now().plusDays(1).toInstant()));
                      System.out.println(Interval.of(ZonedDateTime.now().toInstant(), Duration.ofDays(1)));
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateAbstractInterval() {
        // language=java
        rewriteRun(
          java(
            """
                import org.joda.time.DateTime;
                import org.joda.time.Interval;

                class A {
                    public void foo() {
                        new Interval(50, 100).getStart();
                        new Interval(50, 100).getEnd();
                        new Interval(50, 100).toDuration();
                        new Interval(50, 100).toDurationMillis();
                        new Interval(50, 100).contains(75);
                    }
                }
                """,
            """
              import org.threeten.extra.Interval;

              import java.time.Instant;
              import java.time.ZoneId;

              class A {
                  public void foo() {
                      Interval.of(Instant.ofEpochMilli(50), Instant.ofEpochMilli(100)).getStart().atZone(ZoneId.systemDefault());
                      Interval.of(Instant.ofEpochMilli(50), Instant.ofEpochMilli(100)).getEnd().atZone(ZoneId.systemDefault());
                      Interval.of(Instant.ofEpochMilli(50), Instant.ofEpochMilli(100)).toDuration();
                      Interval.of(Instant.ofEpochMilli(50), Instant.ofEpochMilli(100)).toDuration().toMillis();
                      Interval.of(Instant.ofEpochMilli(50), Instant.ofEpochMilli(100)).contains(Instant.ofEpochMilli(75));
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateBaseInterval() {
        // language=java
        rewriteRun(
          java(
            """
                import org.joda.time.Interval;

                class A {
                    public void foo() {
                        Interval i = new Interval(50, 100);
                        long s = i.getStartMillis();
                        long e = i.getEndMillis();
                    }
                }
                """,
            """
              import org.threeten.extra.Interval;

              import java.time.Instant;

              class A {
                  public void foo() {
                      Interval i = Interval.of(Instant.ofEpochMilli(50), Instant.ofEpochMilli(100));
                      long s = i.getStart().toEpochMilli();
                      long e = i.getEnd().toEpochMilli();
                  }
              }
              """
          )
        );
    }
}
