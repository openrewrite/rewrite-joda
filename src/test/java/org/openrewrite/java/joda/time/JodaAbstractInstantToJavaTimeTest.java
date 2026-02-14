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
class JodaAbstractInstantToJavaTimeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResource("/META-INF/rewrite/no-joda-time.yml", "org.openrewrite.java.joda.time.NoJodaTime")
          .parser(JavaParser.fromJavaVersion().classpath("joda-time", "threeten-extra"));
    }

    @DocumentExample
    @Test
    void migrateAbstractInstant() {
        // language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.Duration;
              import org.joda.time.Instant;
              import org.joda.time.format.DateTimeFormat;

              class A {
                  public void foo() {
                      new DateTime().equals(DateTime.now());
                      new DateTime().getZone();
                      new DateTime().isAfter(1234567890L);
                      new Instant().isAfter(1234567890L);
                      new DateTime().isAfter(DateTime.now().minusDays(1));
                      new Instant().isAfter(Instant.now().minus(Duration.standardDays(1)));
                      new DateTime().isBefore(1234567890L);
                      new Instant().isBefore(1234567890L);
                      new DateTime().isBefore(DateTime.now().plusDays(1));
                      new Instant().isBefore(Instant.now().plus(Duration.standardDays(1)));
                      new DateTime().isBeforeNow();
                      new DateTime().isEqual(1234567890L);
                      new DateTime().isEqual(DateTime.now().plusDays(1));
                      new DateTime().toDate();
                      new DateTime().toInstant();
                      new DateTime().toString();
                      new DateTime().toString(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss"));
                  }
              }
              """,
            """
              import java.time.Duration;
              import java.time.Instant;
              import java.time.ZoneId;
              import java.time.ZonedDateTime;
              import java.time.format.DateTimeFormatter;
              import java.util.Date;

              class A {
                  public void foo() {
                      ZonedDateTime.now().equals(ZonedDateTime.now());
                      ZonedDateTime.now().getZone();
                      ZonedDateTime.now().isAfter(Instant.ofEpochMilli(1234567890L).atZone(ZoneId.systemDefault()));
                      Instant.now().isAfter(Instant.ofEpochMilli(1234567890L));
                      ZonedDateTime.now().isAfter(ZonedDateTime.now().minusDays(1));
                      Instant.now().isAfter(Instant.now().minus(Duration.ofDays(1)));
                      ZonedDateTime.now().isBefore(Instant.ofEpochMilli(1234567890L).atZone(ZoneId.systemDefault()));
                      Instant.now().isBefore(Instant.ofEpochMilli(1234567890L));
                      ZonedDateTime.now().isBefore(ZonedDateTime.now().plusDays(1));
                      Instant.now().isBefore(Instant.now().plus(Duration.ofDays(1)));
                      ZonedDateTime.now().isBefore(ZonedDateTime.now());
                      ZonedDateTime.now().isEqual(Instant.ofEpochMilli(1234567890L).atZone(ZoneId.systemDefault()));
                      ZonedDateTime.now().isEqual(ZonedDateTime.now().plusDays(1));
                      Date.from(ZonedDateTime.now().toInstant());
                      ZonedDateTime.now().toInstant();
                      ZonedDateTime.now().toString();
                      ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateReadableInstant() {
        // language=java
        rewriteRun(
          java(
            """
              import org.joda.time.Instant;
              import org.joda.time.ReadableInstant;

              class A {
                  public void foo(ReadableInstant ri) {
                      System.out.println(ri);
                  }
                  public void bar() {
                      foo(new Instant());
                  }
              }
              """,
            """
              import java.time.Instant;

              class A {
                  public void foo(Instant ri) {
                      System.out.println(ri);
                  }
                  public void bar() {
                      foo(Instant.now());
                  }
              }
              """
          )
        );
    }

}
