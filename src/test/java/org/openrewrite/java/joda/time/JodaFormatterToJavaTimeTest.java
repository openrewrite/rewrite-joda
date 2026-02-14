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
class JodaFormatterToJavaTimeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResource("/META-INF/rewrite/joda-time.yml", "org.openrewrite.java.joda.time.JodaTimeRecipe")
          .parser(JavaParser.fromJavaVersion().classpath("joda-time", "threeten-extra"));
    }

    @DocumentExample
    @Test
    void migrateDateTimeFormat() {
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.format.DateTimeFormat;

              class A {
                  public void foo() {
                      System.out.println(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
                      System.out.println(DateTimeFormat.shortDate());
                      System.out.println(DateTimeFormat.mediumDate());
                      System.out.println(DateTimeFormat.longDate());
                      System.out.println(DateTimeFormat.fullDate());
                      System.out.println(DateTimeFormat.shortTime());
                      System.out.println(DateTimeFormat.mediumTime());
                      System.out.println(DateTimeFormat.longTime());
                      System.out.println(DateTimeFormat.fullTime());
                      System.out.println(DateTimeFormat.shortDateTime());
                      System.out.println(DateTimeFormat.mediumDateTime());
                      System.out.println(DateTimeFormat.longDateTime());
                      System.out.println(DateTimeFormat.fullDateTime());
                  }
              }
              """,
            """
              import java.time.format.DateTimeFormatter;
              import java.time.format.FormatStyle;

              class A {
                  public void foo() {
                      System.out.println(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
                      System.out.println(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
                      System.out.println(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM));
                      System.out.println(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG));
                      System.out.println(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL));
                      System.out.println(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
                      System.out.println(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM));
                      System.out.println(DateTimeFormatter.ofLocalizedTime(FormatStyle.LONG));
                      System.out.println(DateTimeFormatter.ofLocalizedTime(FormatStyle.FULL));
                      System.out.println(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT));
                      System.out.println(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM));
                      System.out.println(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.LONG));
                      System.out.println(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.FULL));
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateDateTimeFormatter() {
        // language=java
        rewriteRun(
          java(
            """
              import org.joda.time.format.DateTimeFormat;
              import org.joda.time.DateTime;
              import org.joda.time.DateTimeZone;

              class A {
                  public void foo() {
                      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss").parseDateTime("2024-10-25T15:45:00");
                      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss").parseMillis("2024-10-25T15:45:00");
                      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss").print(1234567890L);
                      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss").print(new DateTime());
                      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(DateTimeZone.UTC);
                      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss").withZoneUTC();
                  }
              }
              """,
            """
              import java.time.Instant;
              import java.time.ZoneId;
              import java.time.ZoneOffset;
              import java.time.ZonedDateTime;
              import java.time.format.DateTimeFormatter;

              class A {
                  public void foo() {
                      ZonedDateTime.parse("2024-10-25T15:45:00", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                      ZonedDateTime.parse("2024-10-25T15:45:00", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")).toInstant().toEpochMilli();
                      ZonedDateTime.ofInstant(Instant.ofEpochMilli(1234567890L), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                      ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC);
                      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC);
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateClassesWithFqn() {
        // language=java
        rewriteRun(
          java(
            """
              class A {
                  public void foo() {
                      System.out.println(org.joda.time.format.DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
                  }
              }
              """,
            """
              import java.time.format.DateTimeFormatter;

              class A {
                  public void foo() {
                      System.out.println(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
                  }
              }
              """
          )
        );
    }
}
