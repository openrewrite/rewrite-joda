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
class JodaDurationToJavaTimeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResource("/META-INF/rewrite/no-joda-time.yml", "org.openrewrite.java.joda.time.NoJodaTime")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "joda-time-2", "threeten-extra-1"));
    }

    @DocumentExample
    @Test
    void migrateJodaDuration() {
        // language=java
        rewriteRun(
          java(
            """
              import org.joda.time.Duration;

              class A {
                  public void foo() {
                      Duration.standardDays(1L);
                      Duration.standardHours(1L);
                      Duration.standardMinutes(1L);
                      Duration.standardSeconds(1L);
                      Duration.millis(1000L);
                      new Duration(1000L);
                      new Duration(1000L, 2000L);
                      new Duration(1000L).getStandardDays();
                      new Duration(1000L).getStandardHours();
                      new Duration(1000L).getStandardMinutes();
                      new Duration(1000L).getStandardSeconds();
                      new Duration(1000L).toDuration();
                      new Duration(1000L).withMillis(2000L);
                      new Duration(1000L).withDurationAdded(550L, 2);
                      new Duration(1000L).withDurationAdded(new Duration(550L), 2);
                      new Duration(1000L).plus(550L);
                      new Duration(1000L).plus(new Duration(550L));
                      new Duration(1000L).minus(550L);
                      new Duration(1000L).minus(new Duration(550L));
                      new Duration(1000L).multipliedBy(2);
                      new Duration(1000L).dividedBy(2);
                      new Duration(1000L).negated();
                      new Duration(1000L).abs();
                  }
              }
              """,
            """
              import java.time.Duration;
              import java.time.Instant;

              class A {
                  public void foo() {
                      Duration.ofDays(1L);
                      Duration.ofHours(1L);
                      Duration.ofMinutes(1L);
                      Duration.ofSeconds(1L);
                      Duration.ofMillis(1000L);
                      Duration.ofMillis(1000L);
                      Duration.between(Instant.ofEpochMilli(1000L), Instant.ofEpochMilli(2000L));
                      Duration.ofMillis(1000L).toDays();
                      Duration.ofMillis(1000L).toHours();
                      Duration.ofMillis(1000L).toMinutes();
                      Duration.ofMillis(1000L).getSeconds();
                      Duration.ofMillis(1000L);
                      Duration.ofMillis(2000L);
                      Duration.ofMillis(1000L).plusMillis(550L * 2);
                      Duration.ofMillis(1000L).plus(Duration.ofMillis(550L).multipliedBy(2));
                      Duration.ofMillis(1000L).plusMillis(550L);
                      Duration.ofMillis(1000L).plus(Duration.ofMillis(550L));
                      Duration.ofMillis(1000L).minusMillis(550L);
                      Duration.ofMillis(1000L).minus(Duration.ofMillis(550L));
                      Duration.ofMillis(1000L).multipliedBy(2);
                      Duration.ofMillis(1000L).dividedBy(2);
                      Duration.ofMillis(1000L).negated();
                      Duration.ofMillis(1000L).abs();
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateDurationConversionMethods() {
        // language=java
        rewriteRun(
          java(
            """
              import org.joda.time.Duration;

              class A {
                  public void foo() {
                      new Duration(1000L).toStandardDays();
                      new Duration(1000L).toStandardHours();
                      new Duration(1000L).toStandardMinutes();
                      new Duration(1000L).toStandardSeconds();
                  }
              }
              """,
            """
              import java.time.Duration;

              class A {
                  public void foo() {
                      Duration.ofMillis(1000L).toDays();
                      Duration.ofMillis(1000L).toHours();
                      Duration.ofMillis(1000L).toMinutes();
                      Duration.ofMillis(1000L).getSeconds();
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateReadableDuration() {
        // language=java
        rewriteRun(
          java(
            """
              import org.joda.time.Duration;
              import org.joda.time.ReadableDuration;

              class A {
                  public void foo(ReadableDuration rd) {
                      rd.toString();
                  }
                  public void bar() {
                      foo(new Duration(1000));
                  }
              }
              """,
            """
              import java.time.Duration;

              class A {
                  public void foo(Duration rd) {
                      rd.toString();
                  }
                  public void bar() {
                      foo(Duration.ofMillis(1000));
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateBaseDuration() {
        // language=java
        rewriteRun(
          java(
            """
                import org.joda.time.Duration;

                class A {
                    public void foo() {
                        Duration d = new Duration(100);
                        d.getMillis();
                    }
                }
                """,
            """
              import java.time.Duration;

              class A {
                  public void foo() {
                      Duration d = Duration.ofMillis(100);
                      d.toMillis();
                  }
              }
              """
          )
        );
    }
}
