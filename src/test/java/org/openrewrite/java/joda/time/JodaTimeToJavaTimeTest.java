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
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.joda.time.table.JodaTimeMigrationBlockers;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.xml.Assertions.xml;

@SuppressWarnings({"deprecation", "unused"})
class JodaTimeToJavaTimeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new JodaTimeToJavaTime(null))
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "joda-time-2", "threeten-extra-1"));
    }

    @DocumentExample
    @Test
    void leaveFileOnJodaTimeWhenNotEverythingCanBeMigrated() {
        rewriteRun(
          spec -> spec.dataTable(JodaTimeMigrationBlockers.Row.class, rows ->
            assertThat(rows).extracting(JodaTimeMigrationBlockers.Row::getReason)
              .anyMatch(reason -> reason.contains("MutableDateTime"))),
          //language=java
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.MutableDateTime;

              class A {
                  int minute(DateTime dt) {
                      MutableDateTime m = dt.toMutableDateTime();
                      return m.getMinuteOfHour() + dt.getHourOfDay();
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveFilesOnJodaTimeThatShareSignaturesWithABlockedFile() {
        rewriteRun(
          spec -> spec.dataTable(JodaTimeMigrationBlockers.Row.class, rows ->
            assertThat(rows).extracting(JodaTimeMigrationBlockers.Row::getSourcePath).containsExactlyInAnyOrder("A.java", "B.java")),
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              class A {
                  DateTime created() {
                      return DateTime.now();
                  }
              }
              """
          ),
          //language=java
          java(
            """
              class B {
                  Object mutable(A a) {
                      return a.created().toMutableDateTime();
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateFilesThatShareSignaturesTogether() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              class A {
                  DateTime created() {
                      return DateTime.now();
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;

              class A {
                  ZonedDateTime created() {
                      return ZonedDateTime.now();
                  }
              }
              """
          ),
          //language=java
          java(
            """
              class B {
                  long millis(A a) {
                      return a.created().getMillis();
                  }
              }
              """,
            """
              class B {
                  long millis(A a) {
                      return a.created().toInstant().toEpochMilli();
                  }
              }
              """
          )
        );
    }

    @Test
    void anonymousClassesArePartOfTheirFile() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              enum Unit {
                  DAY {
                      @Override
                      DateTime truncate(DateTime time) {
                          return time.withTimeAtStartOfDay();
                      }
                  };

                  abstract DateTime truncate(DateTime time);

                  DateTime next(DateTime time) {
                      return DAY.truncate(time).plusDays(1);
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;

              enum Unit {
                  DAY {
                      @Override
                      ZonedDateTime truncate(ZonedDateTime time) {
                          return time.toLocalDate().atStartOfDay(time.getZone());
                      }
                  };

                  abstract ZonedDateTime truncate(ZonedDateTime time);

                  ZonedDateTime next(ZonedDateTime time) {
                      return DAY.truncate(time).plusDays(1);
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveInterfaceOnJodaTimeWhenItsImplementationIsInheritedFromABlockedClass() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.Instant;

              interface Spread {
                  Instant getTime();
              }
              """
          ),
          //language=java
          java(
            """
              import org.joda.time.Instant;

              abstract class Temporal {
                  static final String TYPE = "org.jadira.usertype.dateandtime.joda.PersistentInstantAsMillisLong";

                  public Instant getTime() {
                      return null;
                  }
              }
              """
          ),
          //language=java
          java(
            """
              class Book extends Temporal implements Spread {
              }
              """
          )
        );
    }

    @Test
    void leaveSupertypeOnJodaTimeWhenASubtypeIsBlocked() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              interface ExportJob {
                  DateTime createdAt();
              }
              """
          ),
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              abstract class SearchExportJob implements ExportJob {
                  static final String JODA = "org.joda.time";

                  DateTime expiresAt() {
                      return createdAt().plusDays(1);
                  }
              }
              """
          )
        );
    }

    @Test
    void generatedTypesBelongToTheTypeTheyAreGeneratedFrom() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "joda-time-2")
            //language=java
            .dependsOn(
              """
                import org.joda.time.DateTime;

                public class AutoValue_Event extends Event {
                    public AutoValue_Event(DateTime timestamp) {
                    }
                }
                """
            )),
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              public abstract class Event {
                  public abstract DateTime timestamp();
              }
              """
          ),
          //language=java
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.MutableDateTime;

              class Events {
                  Event create(MutableDateTime time) {
                      return new AutoValue_Event(time.toDateTime());
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveRuntimeTypeTestsOnJodaTime() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              class FuncTest {
                  long millis(Object o) {
                      if (o instanceof DateTime) {
                          return ((DateTime) o).getMillis();
                      }
                      return 0;
                  }
              }
              """
          ),
          //language=java
          java(
            """
              import org.joda.time.DateTimeZone;

              import java.util.Map;

              class Registry {
                  void register(Map<Class<?>, Object> serializers, Object serializer) {
                      serializers.put(DateTimeZone.class, serializer);
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveHibernateEntitiesWithJodaTimeUserTypeAliasesOnJodaTime() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "joda-time-2")
            //language=java
            .dependsOn(
              """
                package org.hibernate.annotations;

                public @interface Type {
                    String type();
                }
                """
            )),
          //language=xml
          xml(
            """
              <hibernate-mapping>
                  <typedef name="dateTime" class="org.jadira.usertype.dateandtime.joda.PersistentDateTime"/>
              </hibernate-mapping>
              """,
            spec -> spec.path("global.hbm.xml")
          ),
          //language=java
          java(
            """
              import org.hibernate.annotations.Type;
              import org.joda.time.DateTime;

              class PortalCookieImpl {
                  @Type(type = "dateTime")
                  private DateTime created;
              }
              """
          )
        );
    }

    @Test
    void javadocLinkToAJavaTimeField() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.Duration;

              abstract class LookupDataAdapter {
                  /** Use {@link Duration#ZERO} if refresh should be disabled. */
                  public abstract Duration refreshInterval();
              }
              """,
            """
              import java.time.Duration;

              abstract class LookupDataAdapter {
                  /** Use {@link Duration#ZERO} if refresh should be disabled. */
                  public abstract Duration refreshInterval();
              }
              """
          )
        );
    }

    @Test
    void readableInstantBecomesZonedDateTime() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.ReadableInstant;

              class QuarterDetail {
                  long millis(ReadableInstant instant) {
                      return instant.getMillis();
                  }

                  long test() {
                      return millis(DateTime.now());
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;

              class QuarterDetail {
                  long millis(ZonedDateTime instant) {
                      return instant.toInstant().toEpochMilli();
                  }

                  long test() {
                      return millis(ZonedDateTime.now());
                  }
              }
              """
          )
        );
    }

    @Test
    void parseOnlyWhatJavaTimeParsesTheSame() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              class IdGenerator {
                  long origin() {
                      return DateTime.parse("2016-05-10").getMillis();
                  }
              }
              """
          ),
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              class Offsets {
                  DateTime parsed() {
                      return DateTime.parse("2016-05-10T10:15:30+01:00");
                  }

                  DateTime converted() {
                      return new DateTime("2016-05-10T10:15:30+01:00");
                  }
              }
              """,
            """
              import java.time.ZoneId;
              import java.time.ZonedDateTime;

              class Offsets {
                  ZonedDateTime parsed() {
                      return ZonedDateTime.parse("2016-05-10T10:15:30+01:00");
                  }

                  ZonedDateTime converted() {
                      return ZonedDateTime.parse("2016-05-10T10:15:30+01:00").withZoneSameInstant(ZoneId.systemDefault());
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotParseWithFormatterOfUnknownPattern() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.format.DateTimeFormatter;

              class A {
                  DateTime parse(DateTimeFormatter formatter, String s) {
                      return formatter.parseDateTime(s);
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveObjectTypedWriterOnJodaTimeWhenTheReaderCasts() {
        rewriteRun(
          //language=java
          //language=java
          java(
            """
              import java.util.Map;

              import org.joda.time.DateMidnight;

              class CalendarController {
                  long startMillis(Map<String, Object> session) {
                      DateMidnight startDate = (DateMidnight) session.get("startDate");
                      return startDate.getMillis();
                  }
              }
              """
          ),
          //language=java
          //language=java
          java(
            """
              import java.util.Map;

              import org.joda.time.DateMidnight;

              class CalendarControllerTest {
                  void test(Map<String, Object> session) {
                      session.put("startDate", new DateMidnight());
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveCallerOnJodaTimeWhenItPassesAnObjectThatIsTestedWithInstanceof() {
        rewriteRun(
          //language=java
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              class DateValidator {
                  boolean validate(Object value) {
                      return value instanceof DateTime && value.toString().endsWith("Z");
                  }
              }
              """
          ),
          //language=java
          //language=java
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.DateTimeZone;

              class DateValidatorTest {
                  boolean test() {
                      return new DateValidator().validate(new DateTime(DateTimeZone.UTC));
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveTypesNamedInXmlConfigurationOnJodaTime() {
        rewriteRun(
          //language=xml
          //language=xml
          xml(
            """
              <beans>
                  <bean id="portalPropertyEditorRegistrar" class="PortalPropertyEditorRegistrar">
                      <property name="propertyEditors">
                          <map key-type="java.lang.Class">
                              <entry key="org.joda.time.ReadableDuration">
                                  <bean class="ReadableDurationEditor"/>
                              </entry>
                          </map>
                      </property>
                  </bean>
              </beans>
              """,
            spec -> spec.path("applicationContext.xml")
          ),
          //language=java
          //language=java
          java(
            """
              import java.beans.PropertyEditorSupport;

              import org.joda.time.Duration;

              public class ReadableDurationEditor extends PropertyEditorSupport {
                  @Override
                  public void setAsText(String text) {
                      setValue(Duration.parse(text));
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveTypesHandledByJodaNamedClassesOnJodaTime() {
        rewriteRun(
          //language=java
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              import java.util.function.Function;

              public class JodaDateTimeCodec implements Function<String, DateTime> {
                  @Override
                  public DateTime apply(String value) {
                      return DateTime.parse(value);
                  }
              }
              """
          ),
          //language=java
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              public class User {
                  public String name;
                  public DateTime birthday;
              }
              """
          )
        );
    }

    @Test
    void classLiteralAsTypeTokenIsNotARuntimeTypeTest() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "joda-time-2", "threeten-extra-1")
            //language=java
            .dependsOn(
              """
                package org.mockito;

                public class ArgumentMatchers {
                    public static <T> T any(Class<T> type) {
                        return null;
                    }
                }
                """
            )),
          //language=java
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              class Service {
                  String format(DateTime time) {
                      return time.toString();
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;

              class Service {
                  String format(ZonedDateTime time) {
                      return time.toString();
                  }
              }
              """
          ),
          //language=java
          //language=java
          java(
            """
              import org.joda.time.DateTime;
              import org.mockito.ArgumentMatchers;

              class ServiceTest {
                  String test(Service service) {
                      return service.format(ArgumentMatchers.any(DateTime.class));
                  }
              }
              """,
            """
              import org.mockito.ArgumentMatchers;

              import java.time.ZonedDateTime;

              class ServiceTest {
                  String test(Service service) {
                      return service.format(ArgumentMatchers.any(ZonedDateTime.class));
                  }
              }
              """
          )
        );
    }

    @Test
    void hibernateTypedefWithAttributesInAnyOrder() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "joda-time-2")
            //language=java
            .dependsOn(
              """
                package org.hibernate.annotations;

                public @interface Type {
                    String type();
                }
                """
            )),
          //language=xml
          //language=xml
          xml(
            """
              <hibernate-mapping>
                  <typedef class="org.jadira.usertype.dateandtime.joda.PersistentDateTime" name="dateTime"/>
              </hibernate-mapping>
              """,
            spec -> spec.path("global.hbm.xml")
          ),
          //language=java
          //language=java
          java(
            """
              import org.hibernate.annotations.Type;
              import org.joda.time.DateTime;

              class PortalCookieImpl {
                  @Type(type = "dateTime")
                  private DateTime created;
              }
              """
          )
        );
    }

    @Test
    void subclassThatOnlyInheritsAJodaTimeMethodIsNotTiedToIt() {
        rewriteRun(
          spec -> spec.dataTable(JodaTimeMigrationBlockers.Row.class, rows ->
            assertThat(rows).extracting(JodaTimeMigrationBlockers.Row::getSourcePath).containsOnly("Base.java")),
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              class Base {
                  static final String JODA = "org.joda.time";

                  DateTime time() {
                      return DateTime.now();
                  }
              }
              """
          ),
          //language=java
          java(
            """
              class Sub extends Base {
              }
              """
          )
        );
    }

    @Test
    void inheritedTypeTokenMethod() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "joda-time-2", "threeten-extra-1")
            .dependsOn(
              """
                package org.mockito;

                public class ArgumentMatchers {
                    public static <T> T any(Class<T> type) {
                        return null;
                    }
                }
                """,
              """
                package org.mockito;

                public class Mockito extends ArgumentMatchers {
                }
                """
            )),
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              class Service {
                  String format(DateTime time) {
                      return time.toString();
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;

              class Service {
                  String format(ZonedDateTime time) {
                      return time.toString();
                  }
              }
              """
          ),
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              import static org.mockito.Mockito.any;

              class ServiceTest {
                  String test(Service service) {
                      return service.format(any(DateTime.class));
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;

              import static org.mockito.Mockito.any;

              class ServiceTest {
                  String test(Service service) {
                      return service.format(any(ZonedDateTime.class));
                  }
              }
              """
          )
        );
    }

    @Test
    void boundedTypeTokenMethod() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "joda-time-2", "threeten-extra-1")
            .dependsOn(
              """
                package org.mockito;

                public class ArgumentCaptor<T> {
                    public static <U, S extends U> ArgumentCaptor<U> forClass(Class<S> clazz) {
                        return null;
                    }

                    public T getValue() {
                        return null;
                    }
                }
                """
            )),
          //language=java
          java(
            """
              import org.joda.time.DateTimeZone;
              import org.mockito.ArgumentCaptor;

              class ParserTest {
                  String capture() {
                      ArgumentCaptor<DateTimeZone> captor = ArgumentCaptor.forClass(DateTimeZone.class);
                      return captor.getValue().getID();
                  }
              }
              """,
            """
              import org.mockito.ArgumentCaptor;

              import java.time.ZoneId;

              class ParserTest {
                  String capture() {
                      ArgumentCaptor<ZoneId> captor = ArgumentCaptor.forClass(ZoneId.class);
                      return captor.getValue().getId();
                  }
              }
              """
          )
        );
    }

    @Test
    void buildDescriptorsDoNotKeepTypesOnJodaTime() {
        rewriteRun(
          //language=xml
          xml(
            """
              <project>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>de.thetaphi</groupId>
                              <artifactId>forbiddenapis</artifactId>
                              <configuration>
                                  <signatures><![CDATA[
                                      @defaultMessage Constructing a DateTime without a time zone is dangerous
                                      org.joda.time.DateTime#<init>()
                                      org.joda.time.DateTime#now()
                                      org.joda.time.DateTimeZone#getDefault()
                                  ]]></signatures>
                              </configuration>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            spec -> spec.path("pom.xml")
          ),
          //language=java
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.DateTimeZone;

              class Event {
                  DateTime at(DateTimeZone zone) {
                      return DateTime.now(zone);
                  }
              }
              """,
            """
              import java.time.ZoneId;
              import java.time.ZonedDateTime;

              class Event {
                  ZonedDateTime at(ZoneId zone) {
                      return ZonedDateTime.now(zone);
                  }
              }
              """
          )
        );
    }

    @Test
    void staticAnalysisFiltersDoNotKeepTypesOnJodaTime() {
        rewriteRun(
          //language=xml
          xml(
            """
              <FindBugsFilter>
                  <!-- Most Joda time classes are immutable -->
                  <Match>
                      <Field type="org.joda.time.DateTime"/>
                      <Bug pattern="EI_EXPOSE_REP,EI_EXPOSE_REP2"/>
                  </Match>
              </FindBugsFilter>
              """,
            spec -> spec.path("spotbugs-exclude.xml")
          ),
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              class Audit {
                  private final DateTime created;

                  Audit(DateTime created) {
                      this.created = created;
                  }

                  DateTime getCreated() {
                      return created;
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;

              class Audit {
                  private final ZonedDateTime created;

                  Audit(ZonedDateTime created) {
                      this.created = created;
                  }

                  ZonedDateTime getCreated() {
                      return created;
                  }
              }
              """
          )
        );
    }

    @Test
    void jodaNamedUtilityDoesNotKeepTypesOnJodaTime() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              public class JodaUtils {
                  public static DateTime minDateTime(DateTime a, DateTime b) {
                      return a.isBefore(b) ? a : b;
                  }
              }
              """
          ),
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              class ServiceMetricEvent {
                  private final DateTime createdTime;

                  ServiceMetricEvent(DateTime createdTime) {
                      this.createdTime = createdTime;
                  }

                  DateTime getCreatedTime() {
                      return createdTime;
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;

              class ServiceMetricEvent {
                  private final ZonedDateTime createdTime;

                  ServiceMetricEvent(ZonedDateTime createdTime) {
                      this.createdTime = createdTime;
                  }

                  ZonedDateTime getCreatedTime() {
                      return createdTime;
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveTypesMappedWithJodaTimeUserTypesOnJodaTime() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "joda-time-2", "threeten-extra-1")
            .dependsOn(
              """
                package org.hibernate.annotations;

                public @interface Type {
                    String type();
                }
                """,
              """
                package javax.persistence;

                public @interface Entity {
                }
                """
            )),
          //language=java
          java(
            """
              import javax.persistence.Entity;
              import org.hibernate.annotations.Type;
              import org.joda.time.Instant;

              @Entity
              public class Temporal {
                  @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentInstantAsMillisLong")
                  private Instant time;
              }
              """
          ),
          //language=java
          java(
            """
              import javax.persistence.Entity;
              import org.joda.time.Instant;

              @Entity
              public class Adjustment {
                  private Instant timeApplied;

                  public Instant getTimeApplied() {
                      return timeApplied;
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateTypesLookedUpByTypeWhenRequested() {
        rewriteRun(
          spec -> spec.recipe(new JodaTimeToJavaTime(singletonList("org.joda.time.DateTime"))),
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              import java.util.Map;

              class Registry {
                  void register(Map<Class<?>, Object> serializers, Object serializer) {
                      serializers.put(DateTime.class, serializer);
                  }
              }
              """
          ),
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              class Event {
                  DateTime created() {
                      return DateTime.now();
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;

              class Event {
                  ZonedDateTime created() {
                      return ZonedDateTime.now();
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveTypesLookedUpByTypeOnJodaTime() {
        rewriteRun(
          spec -> spec.dataTable(JodaTimeMigrationBlockers.Row.class, rows ->
            assertThat(rows).extracting(JodaTimeMigrationBlockers.Row::getReason)
              .contains("Uses `org.joda.time.DateTime`, which stays on Joda-Time in this repository because it is used as a `Class` value in `Registry.java`")),
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              import java.util.Map;

              class Registry {
                  void register(Map<Class<?>, Object> serializers, Object serializer) {
                      serializers.put(DateTime.class, serializer);
                  }
              }
              """
          ),
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              class Event {
                  DateTime created() {
                      return DateTime.now();
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotCreateADuplicateOverload() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.DateTimeZone;

              import java.time.ZoneId;
              import java.time.ZonedDateTime;

              interface Clock {
                  DateTime now(DateTimeZone zone);

                  ZonedDateTime now(ZoneId zone);
              }
              """
          )
        );
    }

    @Test
    void doNotChangeJodaTimeIntegrations() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              class JodaDateTimeType {
                  boolean isAbleToStore(Object value) {
                      return DateTime.class.isAssignableFrom(value.getClass());
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMigrateDateTimeParsedFromString() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.DateTime;

              class A {
                  DateTime parse(String s) {
                      return new DateTime(s);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotTranslateWeekBasedPatterns() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.format.DateTimeFormat;
              import org.joda.time.format.DateTimeFormatter;

              class A {
                  DateTimeFormatter f() {
                      return DateTimeFormat.forPattern("xxxx-'W'ww");
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeMutableDateTime() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.MutableDateTime;

              class A {
                  int minute(MutableDateTime m) {
                      return m.getMinuteOfHour();
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangePeriodsThatStayJodaTime() {
        rewriteRun(
          //language=java
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.Days;
              import org.joda.time.ReadablePeriod;
              import org.joda.time.Seconds;

              class A {
                  ReadablePeriod units(int n) {
                      return Days.days(n);
                  }

                  Seconds wait(DateTime a, DateTime b) {
                      return Seconds.secondsBetween(a, b);
                  }
              }
              """
          )
        );
    }

}
