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
class JodaDurationToJavaTimeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResource("/META-INF/rewrite/joda-time.yml", "org.openrewrite.java.joda.time.JodaTimeRecipe")
          .parser(JavaParser.fromJavaVersion().classpath("joda-time", "threeten-extra"));
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
                      System.out.println(Duration.standardDays(1L));
                      System.out.println(Duration.standardHours(1L));
                      System.out.println(Duration.standardMinutes(1L));
                      System.out.println(Duration.standardSeconds(1L));
                      System.out.println(Duration.millis(1000L));
                      System.out.println(new Duration(1000L));
                      System.out.println(new Duration(1000L, 2000L));
                      System.out.println(new Duration(1000L).getStandardDays());
                      System.out.println(new Duration(1000L).getStandardHours());
                      System.out.println(new Duration(1000L).getStandardMinutes());
                      System.out.println(new Duration(1000L).getStandardSeconds());
                      System.out.println(new Duration(1000L).toDuration());
                      System.out.println(new Duration(1000L).withMillis(2000L));
                      System.out.println(new Duration(1000L).withDurationAdded(550L, 2));
                      System.out.println(new Duration(1000L).withDurationAdded(new Duration(550L), 2));
                      System.out.println(new Duration(1000L).plus(550L));
                      System.out.println(new Duration(1000L).plus(new Duration(550L)));
                      System.out.println(new Duration(1000L).minus(550L));
                      System.out.println(new Duration(1000L).minus(new Duration(550L)));
                      System.out.println(new Duration(1000L).multipliedBy(2));
                      System.out.println(new Duration(1000L).dividedBy(2));
                      System.out.println(new Duration(1000L).negated());
                      System.out.println(new Duration(1000L).abs());
                  }
              }
              """,
            """
              import java.time.Duration;
              import java.time.Instant;

              class A {
                  public void foo() {
                      System.out.println(Duration.ofDays(1L));
                      System.out.println(Duration.ofHours(1L));
                      System.out.println(Duration.ofMinutes(1L));
                      System.out.println(Duration.ofSeconds(1L));
                      System.out.println(Duration.ofMillis(1000L));
                      System.out.println(Duration.ofMillis(1000L));
                      System.out.println(Duration.between(Instant.ofEpochMilli(1000L), Instant.ofEpochMilli(2000L)));
                      System.out.println(Duration.ofMillis(1000L).toDays());
                      System.out.println(Duration.ofMillis(1000L).toHours());
                      System.out.println(Duration.ofMillis(1000L).toMinutes());
                      System.out.println(Duration.ofMillis(1000L).getSeconds());
                      System.out.println(Duration.ofMillis(1000L));
                      System.out.println(Duration.ofMillis(2000L));
                      System.out.println(Duration.ofMillis(1000L).plusMillis(550L * 2));
                      System.out.println(Duration.ofMillis(1000L).plus(Duration.ofMillis(550L).multipliedBy(2)));
                      System.out.println(Duration.ofMillis(1000L).plusMillis(550L));
                      System.out.println(Duration.ofMillis(1000L).plus(Duration.ofMillis(550L)));
                      System.out.println(Duration.ofMillis(1000L).minusMillis(550L));
                      System.out.println(Duration.ofMillis(1000L).minus(Duration.ofMillis(550L)));
                      System.out.println(Duration.ofMillis(1000L).multipliedBy(2));
                      System.out.println(Duration.ofMillis(1000L).dividedBy(2));
                      System.out.println(Duration.ofMillis(1000L).negated());
                      System.out.println(Duration.ofMillis(1000L).abs());
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
                      System.out.println(new Duration(1000L).toStandardDays());
                      System.out.println(new Duration(1000L).toStandardHours());
                      System.out.println(new Duration(1000L).toStandardMinutes());
                      System.out.println(new Duration(1000L).toStandardSeconds());
                  }
              }
              """,
            """
              import java.time.Duration;

              class A {
                  public void foo() {
                      System.out.println(Duration.ofMillis(1000L).toDays());
                      System.out.println(Duration.ofMillis(1000L).toHours());
                      System.out.println(Duration.ofMillis(1000L).toMinutes());
                      System.out.println(Duration.ofMillis(1000L).getSeconds());
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
                      System.out.println(rd);
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
                      System.out.println(rd);
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
