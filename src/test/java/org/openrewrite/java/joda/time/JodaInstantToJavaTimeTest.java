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
class JodaInstantToJavaTimeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResource("/META-INF/rewrite/no-joda-time.yml", "org.openrewrite.java.joda.time.NoJodaTime")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "joda-time-2", "threeten-extra-1"));
    }

    @DocumentExample
    @Test
    void migrateInstant() {
        // language=java
        rewriteRun(
          java(
            """
              import org.joda.time.Instant;
              import org.joda.time.Duration;

              class A {
                  public void foo() {
                      new Instant();
                      Instant.now().getMillis();
                      Instant.now().minus(Duration.standardDays(1L));
                      Instant.ofEpochMilli(1234567890L);
                      Instant.parse("2024-10-25T15:45:00");
                      Instant.now().plus(Duration.standardDays(1L));
                  }
              }
              """,
            """
              import java.time.Duration;
              import java.time.Instant;

              class A {
                  public void foo() {
                      Instant.now();
                      Instant.now().toEpochMilli();
                      Instant.now().minus(Duration.ofDays(1L));
                      Instant.ofEpochMilli(1234567890L);
                      Instant.parse("2024-10-25T15:45:00");
                      Instant.now().plus(Duration.ofDays(1L));
                  }
              }
              """
          )
        );
    }
}
