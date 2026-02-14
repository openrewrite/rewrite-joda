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
class JodaLocalDateToJavaTimeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResource("/META-INF/rewrite/joda-time.yml", "org.openrewrite.java.joda.time.JodaTimeRecipe")
          .parser(JavaParser.fromJavaVersion().classpath("joda-time", "threeten-extra"));
    }

    @DocumentExample
    @Test
    void migrateLocalDate() {
        // language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTimeZone;
              import org.joda.time.LocalDate;
              import org.joda.time.LocalTime;

              class A {
                  public void foo() {
                      new LocalDate();
                      new LocalDate(DateTimeZone.UTC);
                      new LocalDate(2024, 10, 25);
                      new LocalDate(1234567890L);
                      new LocalDate(1234567890L, DateTimeZone.UTC);
                      LocalDate.now().getDayOfWeek();
                      LocalDate.now().getMonthOfYear();
                      LocalDate.now().withMonthOfYear(6);
                      LocalDate.now().plusDays(1);
                      LocalDate.now().toDateTimeAtStartOfDay();
                      LocalDate.now().toDateTimeAtStartOfDay(DateTimeZone.UTC);
                      LocalDate.now().toLocalDateTime(new LocalTime(10, 30));
                  }
              }
              """,
            """
              import java.time.*;

              class A {
                  public void foo() {
                      LocalDate.now();
                      LocalDate.now(ZoneOffset.UTC);
                      LocalDate.of(2024, 10, 25);
                      Instant.ofEpochMilli(1234567890L).atZone(ZoneId.systemDefault()).toLocalDate();
                      Instant.ofEpochMilli(1234567890L).atZone(ZoneOffset.UTC).toLocalDate();
                      LocalDate.now().getDayOfWeek().getValue();
                      LocalDate.now().getMonthValue();
                      LocalDate.now().withMonth(6);
                      LocalDate.now().plusDays(1);
                      LocalDate.now().atStartOfDay(ZoneId.systemDefault());
                      LocalDate.now().atStartOfDay(ZoneOffset.UTC);
                      LocalDate.now().atTime(LocalTime.of(10, 30));
                  }
              }
              """
          )
        );
    }
}
