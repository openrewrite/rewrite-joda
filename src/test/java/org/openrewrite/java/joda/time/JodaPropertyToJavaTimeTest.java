/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class JodaPropertyToJavaTimeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResource("/META-INF/rewrite/no-joda-time.yml", "org.openrewrite.java.joda.time.NoJodaTime")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "joda-time-2", "threeten-extra-1"));
    }

    @Test
    void propertyIdioms() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.LocalDate;

              import java.util.Locale;

              class A {
                  LocalDate endOfMonth(LocalDate d) {
                      return d.dayOfMonth().withMaximumValue();
                  }

                  DateTime hour(DateTime d) {
                      return d.hourOfDay().roundFloorCopy();
                  }

                  String month(LocalDate d, Locale locale) {
                      return d.monthOfYear().getAsText(locale);
                  }
              }
              """,
            """
              import java.time.LocalDate;
              import java.time.ZonedDateTime;
              import java.time.format.TextStyle;
              import java.time.temporal.ChronoUnit;
              import java.time.temporal.TemporalAdjusters;
              import java.util.Locale;

              class A {
                  LocalDate endOfMonth(LocalDate d) {
                      return d.with(TemporalAdjusters.lastDayOfMonth());
                  }

                  ZonedDateTime hour(ZonedDateTime d) {
                      return d.truncatedTo(ChronoUnit.HOURS);
                  }

                  String month(LocalDate d, Locale locale) {
                      return d.getMonth().getDisplayName(TextStyle.FULL, locale);
                  }
              }
              """
          )
        );
    }
}
