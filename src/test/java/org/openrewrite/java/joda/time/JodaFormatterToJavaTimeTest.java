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

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

@Execution(ExecutionMode.SAME_THREAD)
class JodaFormatterToJavaTimeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResource("/META-INF/rewrite/no-joda-time.yml", "org.openrewrite.java.joda.time.NoJodaTime")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "joda-time-2", "threeten-extra-1"));
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
                      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                      DateTimeFormat.shortDate();
                      DateTimeFormat.mediumDate();
                      DateTimeFormat.longDate();
                      DateTimeFormat.fullDate();
                      DateTimeFormat.shortTime();
                      DateTimeFormat.mediumTime();
                      DateTimeFormat.longTime();
                      DateTimeFormat.fullTime();
                      DateTimeFormat.shortDateTime();
                      DateTimeFormat.mediumDateTime();
                      DateTimeFormat.longDateTime();
                      DateTimeFormat.fullDateTime();
                  }
              }
              """,
            """
              import java.time.format.DateTimeFormatter;
              import java.time.format.FormatStyle;

              class A {
                  public void foo() {
                      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                      DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
                      DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
                      DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG);
                      DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL);
                      DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
                      DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
                      DateTimeFormatter.ofLocalizedTime(FormatStyle.LONG);
                      DateTimeFormatter.ofLocalizedTime(FormatStyle.FULL);
                      DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT);
                      DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM);
                      DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.LONG);
                      DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.FULL);
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
                      ZonedDateTime.parse("2024-10-25T15:45:00", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneId.systemDefault()));
                      ZonedDateTime.parse("2024-10-25T15:45:00", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneId.systemDefault())).toInstant().toEpochMilli();
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
                      org.joda.time.format.DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                  }
              }
              """,
            """
              import java.time.format.DateTimeFormatter;

              class A {
                  public void foo() {
                      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                  }
              }
              """
          )
        );
    }

    @Test
    void translatePatterns() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.format.DateTimeFormat;
              import org.joda.time.format.DateTimeFormatter;

              class A {
                  DateTimeFormatter f() {
                      return DateTimeFormat.forPattern("dd/MM/YYYY HH:mm:ss ZZ [ZZZ]");
                  }

                  String print(DateTime dt) {
                      return dt.toString("YYYY-MM-dd");
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;
              import java.time.format.DateTimeFormatter;

              class A {
                  DateTimeFormatter f() {
                      return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss xxx '['VV']'");
                  }

                  String print(ZonedDateTime dt) {
                      return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                  }
              }
              """
          )
        );
    }

    @Test
    void translatePatternLetters() {
        assertThat(JodaFormatterToJavaTime.translatePattern("dd/MM/YYYY HH:mm:ss ZZ [ZZZ]")).isEqualTo("dd/MM/yyyy HH:mm:ss xxx '['VV']'");
        assertThat(JodaFormatterToJavaTime.translatePattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")).isEqualTo("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        assertThat(JodaFormatterToJavaTime.translatePattern("EEEEE, MMMMM d")).isEqualTo("EEEE, MMMM d");
        assertThat(JodaFormatterToJavaTime.translatePattern("HH:mm:sss")).isNull();
        assertThat(JodaFormatterToJavaTime.translatePattern("xxxx-'W'ww-e")).isNull();
    }

    @Test
    void isoFormats() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.format.ISODateTimeFormat;

              class A {
                  String print(DateTime dt) {
                      return ISODateTimeFormat.dateTime().print(dt);
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;
              import java.time.format.DateTimeFormatter;

              class A {
                  String print(ZonedDateTime dt) {
                      return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
                  }
              }
              """
          )
        );
    }

    @Test
    void parseWithFormatters() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.LocalDate;
              import org.joda.time.format.DateTimeFormat;
              import org.joda.time.format.DateTimeFormatter;

              class A {
                  private static final DateTimeFormatter FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
                  private static final DateTimeFormatter UTC_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").withZoneUTC();

                  LocalDate localDate(String s) {
                      return FORMAT.parseLocalDate(s);
                  }

                  DateTime dateTime(String s) {
                      return FORMAT.parseDateTime(s);
                  }

                  DateTime utc(String s) {
                      return UTC_FORMAT.parseDateTime(s);
                  }
              }
              """,
            """
              import java.time.LocalDate;
              import java.time.ZoneId;
              import java.time.ZoneOffset;
              import java.time.ZonedDateTime;
              import java.time.format.DateTimeFormatter;

              class A {
                  private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                  private static final DateTimeFormatter UTC_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

                  LocalDate localDate(String s) {
                      return LocalDate.parse(s, FORMAT);
                  }

                  ZonedDateTime dateTime(String s) {
                      return ZonedDateTime.parse(s, FORMAT.withZone(ZoneId.systemDefault()));
                  }

                  ZonedDateTime utc(String s) {
                      return ZonedDateTime.parse(s, UTC_FORMAT);
                  }
              }
              """
          )
        );
    }

    @Test
    void parseDateOnlyPatternAtStartOfDay() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.format.DateTimeFormat;
              import org.joda.time.format.DateTimeFormatter;

              class A {
                  static final DateTimeFormatter FMT = DateTimeFormat.forPattern("yyyy-MM-dd");

                  DateTime parse(String s) {
                      return FMT.parseDateTime(s);
                  }
              }
              """,
            """
              import java.time.LocalDate;
              import java.time.ZoneId;
              import java.time.ZonedDateTime;
              import java.time.format.DateTimeFormatter;

              class A {
                  static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                  ZonedDateTime parse(String s) {
                      return LocalDate.parse(s, FMT).atStartOfDay(ZoneId.systemDefault());
                  }
              }
              """
          )
        );
    }
}
