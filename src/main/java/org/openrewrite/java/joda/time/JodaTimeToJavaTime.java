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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.dependencies.AddDependency;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.joda.time.table.JodaTimeMigrationBlockers;
import org.openrewrite.java.tree.*;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;

/// Migrates Joda-Time to `java.time`, but only where the result is complete.
///
/// Every source file that uses Joda-Time is migrated in a dry run first, and the result is checked
/// against the real `java.time` API: no Joda-Time type may remain, every method called on a `java.time`
/// type must exist with compatible arguments and return type, and `java.time` values must only be
/// assigned where their type allows. Source files that pass Joda-Time values to each other through
/// method signatures or fields are then migrated together or not at all, so a migrated declaration
/// never meets a caller or an override that was left on Joda-Time.
@Value
@EqualsAndHashCode(callSuper = false)
public class JodaTimeToJavaTime extends ScanningRecipe<JodaTimeToJavaTime.Accumulator> {

    private static final List<Recipe> MIGRATION = Arrays.asList(
            // Rename methods first, while their receivers are still typed as Joda-Time
            new ChangeMethodName("org.joda.time.base.AbstractDateTime getHourOfDay()", "getHour", true, null),
            new ChangeMethodName("org.joda.time.base.AbstractDateTime getMinuteOfHour()", "getMinute", true, null),
            new ChangeMethodName("org.joda.time.base.AbstractDateTime getSecondOfMinute()", "getSecond", true, null),
            new ChangeMethodName("org.joda.time.base.AbstractDateTime getMonthOfYear()", "getMonthValue", true, null),
            new ChangeMethodName("org.joda.time.DateTime withZone(org.joda.time.DateTimeZone)", "withZoneSameInstant", null, null),
            new ChangeMethodName("org.joda.time.DateTime withZoneRetainFields(org.joda.time.DateTimeZone)", "withZoneSameLocal", null, null),
            new ChangeMethodName("org.joda.time.DateTime withMonthOfYear(int)", "withMonth", null, null),
            new ChangeMethodName("org.joda.time.DateTime withHourOfDay(int)", "withHour", null, null),
            new ChangeMethodName("org.joda.time.DateTime withMinuteOfHour(int)", "withMinute", null, null),
            new ChangeMethodName("org.joda.time.DateTime withSecondOfMinute(int)", "withSecond", null, null),
            new ChangeMethodName("org.joda.time.Duration standardDays(long)", "ofDays", null, null),
            new ChangeMethodName("org.joda.time.Duration standardHours(long)", "ofHours", null, null),
            new ChangeMethodName("org.joda.time.Duration standardMinutes(long)", "ofMinutes", null, null),
            new ChangeMethodName("org.joda.time.Duration standardSeconds(long)", "ofSeconds", null, null),
            new ChangeMethodName("org.joda.time.Duration millis(long)", "ofMillis", null, null),
            new ChangeMethodName("org.joda.time.Duration getStandardDays()", "toDays", null, null),
            new ChangeMethodName("org.joda.time.Duration getStandardHours()", "toHours", null, null),
            new ChangeMethodName("org.joda.time.Duration getStandardMinutes()", "toMinutes", null, null),
            new ChangeMethodName("org.joda.time.Duration getStandardSeconds()", "getSeconds", null, null),
            new ChangeMethodName("org.joda.time.DateTimeZone forID(String)", "of", null, null),
            new ChangeMethodName("org.joda.time.Instant getMillis()", "toEpochMilli", null, null),
            new ChangeMethodName("org.joda.time.base.BaseDuration getMillis()", "toMillis", true, null),
            new ChangeMethodName("org.joda.time.ReadableDuration getMillis()", "toMillis", null, null),
            new ChangeMethodName("org.joda.time.LocalDate getMonthOfYear()", "getMonthValue", null, null),
            new ChangeMethodName("org.joda.time.LocalDate withMonthOfYear(int)", "withMonth", null, null),
            new ChangeMethodName("org.joda.time.LocalTime getHourOfDay()", "getHour", null, null),
            new ChangeMethodName("org.joda.time.LocalTime getMinuteOfHour()", "getMinute", null, null),
            new ChangeMethodName("org.joda.time.LocalTime getSecondOfMinute()", "getSecond", null, null),
            new ChangeMethodName("org.joda.time.LocalTime withHourOfDay(int)", "withHour", null, null),
            new ChangeMethodName("org.joda.time.LocalTime withMinuteOfHour(int)", "withMinute", null, null),
            new ChangeMethodName("org.joda.time.LocalTime withSecondOfMinute(int)", "withSecond", null, null),

            // Then rewrite what needs more than a new name
            new JodaDateTimeToJavaTime(),
            new JodaAbstractInstantToJavaTime(),
            new JodaDurationToJavaTime(),
            new JodaIntervalToJavaTime(),
            new JodaLocalDateToJavaTime(),
            new JodaLocalTimeToJavaTime(),
            new JodaFormatterToJavaTime(),
            new JodaDateTimeZoneToJavaTime(),
            new JodaDateMidnightToJavaTime(),
            new JodaInstantToJavaTime(),
            new JodaTimePeriodToJavaTime(),
            new JodaPropertyToJavaTime(),

            // And change the types last
            new ChangeType("org.joda.time.DateTime", "java.time.ZonedDateTime", null),
            new ChangeType("org.joda.time.ReadableDateTime", "java.time.ZonedDateTime", null),
            new ChangeType("org.joda.time.base.BaseDateTime", "java.time.ZonedDateTime", null),
            new ChangeType("org.joda.time.base.AbstractDateTime", "java.time.ZonedDateTime", null),
            new ChangeType("org.joda.time.DateMidnight", "java.time.ZonedDateTime", null),
            new ChangeType("org.joda.time.DateTimeZone", "java.time.ZoneId", null),
            new ChangeType("org.joda.time.format.DateTimeFormatter", "java.time.format.DateTimeFormatter", null),
            new ChangeType("org.joda.time.format.DateTimeFormat", "java.time.format.DateTimeFormatter", null),
            new ChangeType("org.joda.time.Duration", "java.time.Duration", null),
            new ChangeType("org.joda.time.ReadableDuration", "java.time.Duration", null),
            new ChangeType("org.joda.time.base.BaseDuration", "java.time.Duration", null),
            new ChangeType("org.joda.time.base.AbstractDuration", "java.time.Duration", null),
            new ChangeType("org.joda.time.Instant", "java.time.Instant", null),
            new ChangeType("org.joda.time.base.AbstractInstant", "java.time.Instant", null),
            new ChangeType("org.joda.time.ReadableInstant", "java.time.ZonedDateTime", null),
            new ChangeType("org.joda.time.Interval", "org.threeten.extra.Interval", null),
            new ChangeType("org.joda.time.ReadableInterval", "org.threeten.extra.Interval", null),
            new ChangeType("org.joda.time.base.BaseInterval", "org.threeten.extra.Interval", null),
            new ChangeType("org.joda.time.base.AbstractInterval", "org.threeten.extra.Interval", null),
            new ChangeType("org.joda.time.LocalDate", "java.time.LocalDate", null),
            new ChangeType("org.joda.time.LocalTime", "java.time.LocalTime", null),
            new ChangeType("org.joda.time.LocalDateTime", "java.time.LocalDateTime", null)
    );

    private static final Map<String, Optional<Class<?>>> CLASSES = new ConcurrentHashMap<>();

    private static final MethodMatcher DATE_TIME_PARSE_FORMATTER = new MethodMatcher("org.joda.time.DateTime parse(String, org.joda.time.format.DateTimeFormatter)");

    private static final Pattern XML_TYPEDEF = Pattern.compile("<typedef\\b([^>]*)>");
    private static final Pattern XML_TYPEDEF_NAME = Pattern.compile("\\bname\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern XML_TYPEDEF_CLASS = Pattern.compile("\\bclass\\s*=\\s*\"([^\"]+)\"");
    private static final String JODA_TIME_TYPE = "org\\.joda\\.time(?:\\.[a-z]+)*\\.[A-Z]\\w*";
    private static final Pattern XML_JODA_TIME_TYPE_REFERENCE = Pattern.compile(
            "\\b((?:key|value|class|type|key-type|value-type|javaType|returnType)\\s*=\\s*\"(" + JODA_TIME_TYPE + ")\")" +
            "|(<(?:key|value|class|type)>\\s*(" + JODA_TIME_TYPE + ")\\s*</)");
    private static final Pattern XML_MAPPED_PROPERTY_TYPE = Pattern.compile(
            "<(?:property|element|id|key-property|version|timestamp)\\b[^>]*\\btype\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern JADIRA_PERSISTED_TYPE = Pattern.compile("Persistent([A-Z][A-Za-z]*)");
    private static final List<String> JODA_TIME_SIMPLE_NAMES = Arrays.asList(
            "LocalDateTime", "LocalDate", "LocalTime", "DateTimeZone", "DateTime", "DateMidnight", "Instant",
            "Interval", "Duration", "Period", "MonthDay", "YearMonth");
    private static final Pattern XML_COMMENT_OR_CDATA = Pattern.compile("<!--.*?-->|<!\\[CDATA\\[.*?]]>", Pattern.DOTALL);
    private static final Pattern XML_NOT_CONFIGURATION = Pattern.compile(
            "(^|/)pom\\.xml$|spotbugs|findbugs|checkstyle|pmd|suppressions|forbidden|ruleset", Pattern.CASE_INSENSITIVE);

    @Option(displayName = "Joda-Time types to migrate anyway",
            description = "Fully qualified Joda-Time types to migrate even where the repository registers or looks them up " +
                          "by their type, which otherwise keeps them on Joda-Time throughout the repository. Use this after " +
                          "reviewing the registrations named in the Joda-Time migration blockers data table. The files " +
                          "that hold those registrations stay on Joda-Time, so update them by hand, including the files " +
                          "that convert or bind values of these types.",
            example = "org.joda.time.DateTime",
            required = false)
    @Nullable
    List<String> migrateDespiteTypeLookups;

    String displayName = "Migrate Joda-Time to `java.time` where it can be done completely";

    String description = "Migrates Joda-Time types and method calls to `java.time` and ThreeTen-Extra. " +
            "Source files that pass Joda-Time values to each other through method signatures, fields or supertypes are " +
            "migrated together, and only when every Joda-Time usage in all of them has a `java.time` mapping, so that " +
            "the result compiles. To limit changes in behaviour, a Joda-Time type also stays on Joda-Time throughout " +
            "the repository when it is used as a `Class` value other than a type token, handled by a framework " +
            "integration named after Joda-Time, mapped with a Joda-Time user type, or named in framework XML " +
            "configuration; and files that test runtime types with `instanceof` or a cast, or that pass such values as " +
            "`Object`, are left on Joda-Time too. These are heuristics, not a guarantee " +
            "that behaviour is unchanged: review the migrated code where values are converted to text, persisted or " +
            "looked up by type, as for example `ZonedDateTime.toString()` and `ZoneOffset.UTC.getId()` differ from " +
            "their Joda-Time counterparts. Everything that is left on Joda-Time is listed with the reason in the " +
            "Joda-Time migration blockers data table.";

    transient JodaTimeMigrationBlockers blockers = new JodaTimeMigrationBlockers(this);

    @Override
    public List<Recipe> getRecipeList() {
        // in the next cycle, where the migrated sources use ThreeTen-Extra
        return singletonList(new AddDependency("org.threeten", "threeten-extra", "1.8.0", null,
                "org.threeten.extra.Interval", null, null, null, null, null, null, null, null, null));
    }

    @Override
    public boolean causesAnotherCycle() {
        // so that dependencies can be added for the `java.time` and ThreeTen-Extra types now in use
        return true;
    }

    public static class Accumulator {
        final Map<String, Path> typeDeclarations = new HashMap<>();
        final Map<Path, Candidate> candidates = new HashMap<>();
        final Set<Path> reported = new HashSet<>();
        /// Hibernate type aliases of Joda-Time user types, mapped to the user type.
        final Map<String, String> jodaTypeAliases = new HashMap<>();

        /// Hibernate type aliases that XML mappings use for properties, per XML file.
        final Map<Path, Set<String>> xmlTypeAliasReferences = new HashMap<>();

        /// Joda-Time types named in XML configuration, per XML file.
        final Map<Path, Map<String, String>> xmlPinnedTypes = new HashMap<>();

        /// Joda-Time types that the user chose to migrate even where they are looked up by type.
        final Set<String> migrateDespiteTypeLookups;

        Accumulator(Set<String> migrateDespiteTypeLookups) {
            this.migrateDespiteTypeLookups = migrateDespiteTypeLookups;
        }

        @Nullable
        Map<Path, List<String>> blocked;

        Map<Path, List<String>> blocked() {
            if (blocked == null) {
                Map<Path, Path> components = new HashMap<>();
                Map<Path, List<String>> problems = new TreeMap<>();
                Map<String, String> pinnedTypes = new TreeMap<>();
                Map<String, String> runtimeTestedTypes = new TreeMap<>();
                for (Map<String, String> xmlPins : xmlPinnedTypes.values()) {
                    xmlPins.forEach(pinnedTypes::putIfAbsent);
                }
                for (Candidate candidate : candidates.values()) {
                    candidate.pinnedTypes.forEach(pinnedTypes::putIfAbsent);
                    candidate.runtimeTestedTypes.forEach(runtimeTestedTypes::putIfAbsent);
                }
                // a Hibernate user type for Joda-Time maps every member of that type, wherever it is referenced
                for (Map.Entry<Path, Candidate> entry : candidates.entrySet()) {
                    for (Map.Entry<String, String> aliased : entry.getValue().aliasedTypes.entrySet()) {
                        if (jodaTypeAliases.containsKey(aliased.getKey())) {
                            pinnedTypes.putIfAbsent(aliased.getValue(), "is mapped with the Hibernate type `" + aliased.getKey() + "` in `" + entry.getKey() + "`");
                        }
                    }
                }
                for (Map.Entry<Path, Set<String>> references : xmlTypeAliasReferences.entrySet()) {
                    for (String alias : references.getValue()) {
                        String jodaType = jodaTimeTypeOfUserType(jodaTypeAliases.get(alias));
                        if (jodaType != null) {
                            pinnedTypes.putIfAbsent(jodaType, "is mapped with the Hibernate type `" + alias + "` in `" + references.getKey() + "`");
                        }
                    }
                }
                pinnedTypes.keySet().removeAll(migrateDespiteTypeLookups);
                runtimeTestedTypes.keySet().removeAll(migrateDespiteTypeLookups);
                for (Map.Entry<Path, Candidate> entry : candidates.entrySet()) {
                    Candidate candidate = entry.getValue();
                    List<String> fileProblems = new ArrayList<>(candidate.problems);
                    for (JavaType.FullyQualified jodaType : candidate.jodaTypes.values()) {
                        if (migrateDespiteTypeLookups.contains(jodaType.getFullyQualifiedName())) {
                            continue;
                        }
                        for (Map.Entry<String, String> pinned : pinnedTypes.entrySet()) {
                            if (!candidate.pinnedTypes.containsKey(pinned.getKey()) && TypeUtils.isAssignableTo(pinned.getKey(), jodaType)) {
                                fileProblems.add("Uses `" + jodaType.getFullyQualifiedName() + "`, which stays on Joda-Time in this repository because " +
                                                 (pinned.getKey().equals(jodaType.getFullyQualifiedName()) ? "it" : "`" + pinned.getKey() + "`") +
                                                 " " + pinned.getValue());
                                break;
                            }
                        }
                    }
                    for (JavaType.FullyQualified passed : candidate.passedAsObject.values()) {
                        if (migrateDespiteTypeLookups.contains(passed.getFullyQualifiedName())) {
                            continue;
                        }
                        for (Map.Entry<String, String> tested : runtimeTestedTypes.entrySet()) {
                            if (!candidate.runtimeTestedTypes.containsKey(tested.getKey()) && TypeUtils.isAssignableTo(tested.getKey(), passed)) {
                                fileProblems.add("Passes a `" + passed.getFullyQualifiedName() + "` as `Object`, while " + tested.getValue());
                                break;
                            }
                        }
                    }
                    for (String alias : candidate.typeAliases) {
                        if (jodaTypeAliases.containsKey(alias)) {
                            fileProblems.add("Maps a field with the Hibernate type `" + alias + "`, which is defined as a Joda-Time user type");
                        }
                    }
                    for (Map.Entry<String, @Nullable String> shared : candidate.sharedTypes.entrySet()) {
                        Path declaredIn = declaringFile(shared.getKey(), typeDeclarations);
                        if (declaredIn == null) {
                            if (shared.getValue() != null) {
                                fileProblems.add("Exchanges Joda-Time values with `" + shared.getValue() + "`, which is not part of this repository");
                            }
                        } else if (candidates.containsKey(declaredIn)) {
                            components.put(find(components, entry.getKey()), find(components, declaredIn));
                        }
                    }
                    problems.put(entry.getKey(), fileProblems);
                }
                Map<Path, Integer> componentSizes = new HashMap<>();
                Map<Path, Integer> blockedInComponent = new HashMap<>();
                Map<Path, Map<String, Integer>> reasonsInComponent = new HashMap<>();
                for (Map.Entry<Path, List<String>> entry : problems.entrySet()) {
                    Path root = find(components, entry.getKey());
                    componentSizes.merge(root, 1, Integer::sum);
                    if (!entry.getValue().isEmpty()) {
                        blockedInComponent.merge(root, 1, Integer::sum);
                        for (String reason : entry.getValue()) {
                            reasonsInComponent.computeIfAbsent(root, r -> new HashMap<>()).merge(reason, 1, Integer::sum);
                        }
                    }
                }
                blocked = new HashMap<>();
                for (Map.Entry<Path, List<String>> entry : problems.entrySet()) {
                    Path root = find(components, entry.getKey());
                    if (!blockedInComponent.containsKey(root)) {
                        continue;
                    }
                    if (!entry.getValue().isEmpty()) {
                        blocked.put(entry.getKey(), entry.getValue());
                        continue;
                    }
                    StringJoiner commonReasons = new StringJoiner("; ");
                    reasonsInComponent.get(root).entrySet().stream()
                            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                            .limit(3)
                            .forEach(reason -> commonReasons.add(reason.getKey() + " (" + reason.getValue() + "x)"));
                    blocked.put(entry.getKey(), singletonList("Exchanges Joda-Time values with a group of " +
                            componentSizes.get(root) + " files, of which " + blockedInComponent.get(root) +
                            " can not be migrated, most often because: " + commonReasons));
                }
            }
            return blocked;
        }

        /// Anonymous and local classes are not recorded, so they resolve to the closest enclosing type that is. Types
        /// generated from a repository type, such as AutoValue implementations and JPA metamodels, resolve to that type.
        private static @Nullable Path declaringFile(String type, Map<String, Path> typeDeclarations) {
            for (String t = type; ; t = t.substring(0, t.lastIndexOf('$'))) {
                Path path = typeDeclarations.get(t);
                if (path == null) {
                    path = generatedFrom(t, typeDeclarations);
                }
                if (path != null || t.lastIndexOf('$') < 0) {
                    return path;
                }
            }
        }

        private static @Nullable Path generatedFrom(String type, Map<String, Path> typeDeclarations) {
            int simpleNameStart = Math.max(type.lastIndexOf('.'), type.lastIndexOf('$')) + 1;
            String packagePrefix = type.substring(0, simpleNameStart);
            String simpleName = type.substring(simpleNameStart);
            List<String> sources = new ArrayList<>();
            for (String prefix : Arrays.asList("AutoValue_", "$AutoValue_", "Immutable")) {
                if (simpleName.startsWith(prefix)) {
                    String generatedFrom = simpleName.substring(prefix.length());
                    sources.add(generatedFrom);
                    sources.add(generatedFrom.replace('_', '$'));
                }
            }
            if (simpleName.endsWith("_")) {
                sources.add(simpleName.substring(0, simpleName.length() - 1));
            }
            for (String source : sources) {
                Path path = typeDeclarations.get(packagePrefix + source);
                if (path != null) {
                    return path;
                }
            }
            return null;
        }

        private static Path find(Map<Path, Path> components, Path path) {
            Path root = path;
            for (Path parent = components.get(root); parent != null && !parent.equals(root); parent = components.get(root)) {
                root = parent;
            }
            return root;
        }
    }

    static class Candidate {
        final Set<String> problems = new LinkedHashSet<>();

        /// Types declared outside this file whose members exchange Joda-Time values with it, mapped to a description of
        /// the member when its declared signature mentions Joda-Time, which matters when the type turns out not to be
        /// part of the repository.
        final Map<String, @Nullable String> sharedTypes = new HashMap<>();

        /// Hibernate type names used in `@Type(type = "...")`, which may be aliases of Joda-Time user types.
        final Set<String> typeAliases = new HashSet<>();

        /// Those Hibernate type names mapped to the Joda-Time type of the member they annotate.
        final Map<String, String> aliasedTypes = new HashMap<>();

        /// The Joda-Time types this file uses, to find out whether it uses one that must stay on Joda-Time.
        final Map<String, JavaType.FullyQualified> jodaTypes = new HashMap<>();

        /// Joda-Time types passed to parameters of type `Object`, where a runtime type test may expect them.
        final Map<String, JavaType.FullyQualified> passedAsObject = new HashMap<>();

        /// Joda-Time types this file requires to stay on Joda-Time throughout the repository, with the reason.
        final Map<String, String> pinnedTypes = new HashMap<>();

        /// Joda-Time types whose runtime type this file tests, with a description of where.
        final Map<String, String> runtimeTestedTypes = new HashMap<>();

        /// Calls of the lenient Joda-Time `parse` methods, which must not survive the migration under a `java.time` name.
        final Set<UUID> jodaParses = new HashSet<>();

        /// The dry run of the migration, reused when the same source file is migrated for real.
        @Nullable WeakReference<Tree> scanned;
        @Nullable SoftReference<Tree> migrated;
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator(migrateDespiteTypeLookups == null ? emptySet() : new HashSet<>(migrateDespiteTypeLookups));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile && ((SourceFile) tree).getSourcePath().toString().endsWith(".xml")) {
                    scanXml((SourceFile) tree, acc);
                }
                if (!(tree instanceof JavaSourceFile)) {
                    return tree;
                }
                JavaSourceFile source = (JavaSourceFile) tree;
                Path path = source.getSourcePath();
                acc.blocked = null;
                if (source.getTypesInUse().hasType("org.hibernate.annotations.TypeDef", false)) {
                    collectTypeDefs(source, acc.jodaTypeAliases);
                }
                acc.candidates.remove(path);

                Set<String> declaredTypes = new HashSet<>();
                List<JavaType.FullyQualified> classTypes = new ArrayList<>();
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Integer p) {
                        if (classDecl.getType() != null) {
                            declaredTypes.add(classDecl.getType().getFullyQualifiedName());
                            classTypes.add(classDecl.getType());
                        }
                        return super.visitClassDeclaration(classDecl, p);
                    }

                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Integer p) {
                        return method;
                    }
                }.visit(source, 0);
                for (String type : declaredTypes) {
                    acc.typeDeclarations.put(type, path);
                }

                Candidate candidate = new Candidate();
                // a class can tie together the Joda-Time signatures of its supertypes without mentioning Joda-Time itself
                for (JavaType.FullyQualified classType : classTypes) {
                    shareSupertypes(classType, candidate, declaredTypes);
                }
                if (!usesJodaTime(source)) {
                    if (!candidate.sharedTypes.isEmpty()) {
                        acc.candidates.put(path, candidate);
                    }
                    return tree;
                }
                scanSharedSignatures(source, candidate, declaredTypes);
                new JavaIsoVisitor<Set<UUID>>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Set<UUID> parses) {
                        if (DATE_TIME_PARSE_FORMATTER.matches(method) ||
                                "parse".equals(method.getSimpleName()) && method.getArguments().size() == 1 &&
                                method.getMethodType() != null && isJodaTime(method.getMethodType().getDeclaringType()) &&
                                !"org.joda.time.Duration".equals(method.getMethodType().getDeclaringType().getFullyQualifiedName()) &&
                                !parsesTheSame(method.getMethodType().getDeclaringType(), method.getArguments().get(0))) {
                            parses.add(method.getId());
                        }
                        return super.visitMethodInvocation(method, parses);
                    }
                }.visit(source, candidate.jodaParses);
                if (source instanceof J.CompilationUnit) {
                    try {
                        Tree migrated = migrate(source, ctx);
                        new MigrationVerifier(candidate).visit(migrated, ctx);
                        candidate.scanned = new WeakReference<>(source);
                        candidate.migrated = new SoftReference<>(migrated);
                    } catch (Exception e) {
                        candidate.problems.add("The migration failed: " + e.getMessage());
                    }
                }
                acc.candidates.put(path, candidate);
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof JavaSourceFile)) {
                    return tree;
                }
                Path path = ((JavaSourceFile) tree).getSourcePath();
                Candidate candidate = acc.candidates.get(path);
                if (candidate == null) {
                    return tree;
                }
                List<String> reasons = acc.blocked().get(path);
                if (reasons == null) {
                    Tree migrated = candidate.migrated == null || candidate.scanned == null || candidate.scanned.get() != tree ?
                            null : candidate.migrated.get();
                    return migrated != null ? migrated : migrate(tree, ctx);
                }
                if (acc.reported.add(path)) {
                    for (String reason : reasons) {
                        blockers.insertRow(ctx, new JodaTimeMigrationBlockers.Row(path.toString(), reason));
                    }
                }
                return tree;
            }
        };
    }

    private static Tree migrate(Tree tree, ExecutionContext ctx) {
        Tree migrated = tree;
        for (Recipe recipe : MIGRATION) {
            migrated = recipe.getVisitor().visit(migrated, ctx);
        }
        return migrated;
    }

    private static boolean usesJodaTime(JavaSourceFile source) {
        for (JavaType type : source.getTypesInUse().getTypesInUse()) {
            if (mentionsJodaTime(type, 0)) {
                return true;
            }
        }
        for (JavaType.Method method : source.getTypesInUse().getUsedMethods()) {
            if (mentionsJodaTime(method, 0)) {
                return true;
            }
        }
        return false;
    }

    private static void collectJodaTypes(@Nullable JavaType type, Map<String, JavaType.FullyQualified> jodaTypes, int depth) {
        if (type == null || depth > 4) {
            return;
        }
        if (type instanceof JavaType.Parameterized) {
            for (JavaType parameter : ((JavaType.Parameterized) type).getTypeParameters()) {
                collectJodaTypes(parameter, jodaTypes, depth + 1);
            }
        } else if (type instanceof JavaType.Array) {
            collectJodaTypes(((JavaType.Array) type).getElemType(), jodaTypes, depth + 1);
        } else if (type instanceof JavaType.GenericTypeVariable) {
            for (JavaType bound : ((JavaType.GenericTypeVariable) type).getBounds()) {
                collectJodaTypes(bound, jodaTypes, depth + 1);
            }
        }
        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
        if (fq != null && isJodaTime(fq)) {
            jodaTypes.putIfAbsent(fq.getFullyQualifiedName(), fq);
        }
    }

    private static boolean isJodaTime(@Nullable JavaType type) {
        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
        return fq != null && fq.getFullyQualifiedName().startsWith("org.joda.time.");
    }

    private static boolean mentionsJodaTime(@Nullable JavaType type, int depth) {
        if (type == null || depth > 4) {
            return false;
        }
        if (type instanceof JavaType.Method) {
            JavaType.Method method = (JavaType.Method) type;
            if (isJodaTime(method.getDeclaringType()) || mentionsJodaTime(method.getReturnType(), depth + 1)) {
                return true;
            }
            for (JavaType parameter : method.getParameterTypes()) {
                if (mentionsJodaTime(parameter, depth + 1)) {
                    return true;
                }
            }
            return false;
        }
        if (type instanceof JavaType.Parameterized) {
            for (JavaType parameter : ((JavaType.Parameterized) type).getTypeParameters()) {
                if (mentionsJodaTime(parameter, depth + 1)) {
                    return true;
                }
            }
        } else if (type instanceof JavaType.Array) {
            return mentionsJodaTime(((JavaType.Array) type).getElemType(), depth + 1);
        } else if (type instanceof JavaType.GenericTypeVariable) {
            for (JavaType bound : ((JavaType.GenericTypeVariable) type).getBounds()) {
                if (mentionsJodaTime(bound, depth + 1)) {
                    return true;
                }
            }
            return false;
        }
        return isJodaTime(type);
    }

    /// Records the types outside this file whose members exchange Joda-Time values with it, and anything that ties
    /// the file to Joda-Time beyond its Java types.
    private static void scanSharedSignatures(JavaSourceFile source, Candidate candidate, Set<String> declaredTypes) {
        TypesInUse typesInUse = source.getTypesInUse();
        for (JavaType type : typesInUse.getTypesInUse()) {
            collectJodaTypes(type, candidate.jodaTypes, 0);
        }
        for (JavaType type : typesInUse.getTypesInUse()) {
            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
            if (fq != null && !isJodaTime(fq) && fq.getFullyQualifiedName().toLowerCase(Locale.ROOT).contains("joda")) {
                candidate.problems.add("Uses `" + fq.getFullyQualifiedName() + "`, which integrates with Joda-Time");
            }
        }
        for (JavaType.Method method : typesInUse.getUsedMethods()) {
            share(method, candidate, declaredTypes);
        }
        for (JavaType.Method method : typesInUse.getDeclaredMethods()) {
            if (mentionsJodaTime(method, 0)) {
                TypeUtils.findOverriddenMethod(method).ifPresent(overridden -> share(overridden, candidate, declaredTypes));
            }
        }
        for (JavaType.Variable variable : typesInUse.getVariables()) {
            if (variable.getOwner() instanceof JavaType.FullyQualified && mentionsJodaTime(variable.getType(), 0)) {
                String owner = ((JavaType.FullyQualified) variable.getOwner()).getFullyQualifiedName();
                if (isShareable(owner, declaredTypes)) {
                    candidate.sharedTypes.putIfAbsent(owner, owner + "." + variable.getName());
                }
            }
        }
        if (source instanceof J.CompilationUnit) {
            new JavaIsoVisitor<Candidate>() {
                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Candidate c) {
                    boolean jodaNamed = classDecl.getSimpleName().contains("Joda");
                    if (jodaNamed) {
                        c.problems.add("Declares `" + classDecl.getSimpleName() + "`, which by its name integrates with Joda-Time");
                    }
                    JavaType.FullyQualified type = classDecl.getType();
                    if (type != null) {
                        List<JavaType.FullyQualified> supertypes = new ArrayList<>(type.getInterfaces());
                        if (type.getSupertype() != null) {
                            supertypes.add(type.getSupertype());
                        }
                        for (JavaType.FullyQualified supertype : supertypes) {
                            if (isJodaTime(supertype)) {
                                c.problems.add("Extends or implements `" + supertype.getFullyQualifiedName() + "`");
                                }
                        }
                    }
                    int typeLookups = c.pinnedTypes.size() + c.runtimeTestedTypes.size();
                    J.ClassDeclaration visited = super.visitClassDeclaration(classDecl, c);
                    // a utility class named after Joda-Time is not an integration; one that plugs into a framework or
                    // looks values up by type is
                    boolean pluggedIn = type != null && (!type.getInterfaces().isEmpty() ||
                            type.getSupertype() != null && !"java.lang.Object".equals(type.getSupertype().getFullyQualifiedName())) ||
                            isRegisteredByAnnotationOrOverload(classDecl);
                    if (jodaNamed && (pluggedIn || c.pinnedTypes.size() + c.runtimeTestedTypes.size() > typeLookups)) {
                        for (String jodaType : c.jodaTypes.keySet()) {
                            c.pinnedTypes.putIfAbsent(jodaType, "is handled by the integration `" + classDecl.getSimpleName() + "` in `" + source.getSourcePath() + "`");
                        }
                    }
                    return visited;
                }

                /// A framework can also find an integration through an annotation, or choose among overloads of one method
                /// by their Joda-Time parameter types, like a template engine choosing a value transformer does.
                private boolean isRegisteredByAnnotationOrOverload(J.ClassDeclaration classDecl) {
                    if (hasFrameworkAnnotation(classDecl.getLeadingAnnotations())) {
                        return true;
                    }
                    Map<String, Integer> jodaOverloads = new HashMap<>();
                    for (Statement statement : classDecl.getBody().getStatements()) {
                        if (statement instanceof J.MethodDeclaration) {
                            J.MethodDeclaration method = (J.MethodDeclaration) statement;
                            if (hasFrameworkAnnotation(method.getLeadingAnnotations())) {
                                return true;
                            }
                            if (method.getMethodType() != null && method.getMethodType().getParameterTypes().stream()
                                    .anyMatch(parameter -> mentionsJodaTime(parameter, 0)) &&
                                jodaOverloads.merge(method.getSimpleName(), 1, Integer::sum) > 1) {
                                return true;
                            }
                        }
                    }
                    return false;
                }

                private boolean hasFrameworkAnnotation(List<J.Annotation> annotations) {
                    for (J.Annotation annotation : annotations) {
                        JavaType.FullyQualified type = TypeUtils.asFullyQualified(annotation.getType());
                        if (type == null || !type.getFullyQualifiedName().startsWith("java.lang.")) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, Candidate c) {
                    JavaType.FullyQualified type = TypeUtils.asFullyQualified(instanceOf.getClazz() instanceof TypedTree ? ((TypedTree) instanceOf.getClazz()).getType() : null);
                    if (type != null && isJodaTime(type)) {
                        c.problems.add("Tests the runtime type `" + type.getFullyQualifiedName() + "` with `instanceof`, which values from code left on Joda-Time still have");
                        c.runtimeTestedTypes.putIfAbsent(type.getFullyQualifiedName(), "`" + source.getSourcePath() + "` tests for it with `instanceof`");
                    }
                    return super.visitInstanceOf(instanceOf, c);
                }

                @Override
                public J.TypeCast visitTypeCast(J.TypeCast typeCast, Candidate c) {
                    JavaType.FullyQualified type = TypeUtils.asFullyQualified(typeCast.getClazz().getTree().getType());
                    if (type != null && isJodaTime(type)) {
                        c.problems.add("Casts to `" + type.getFullyQualifiedName() + "`, which values from code left on Joda-Time still have");
                        c.runtimeTestedTypes.putIfAbsent(type.getFullyQualifiedName(), "`" + source.getSourcePath() + "` casts it");
                    }
                    return super.visitTypeCast(typeCast, c);
                }

                @Override
                public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Candidate c) {
                    JavaType.FullyQualified type = TypeUtils.asFullyQualified(fieldAccess.getTarget().getType());
                    if ("class".equals(fieldAccess.getSimpleName()) && type != null && isJodaTime(type) && !isTypeToken(fieldAccess)) {
                        c.problems.add("Uses `" + type.getFullyQualifiedName() + ".class` as a value, for example to register or look up Joda-Time values by their type");
                        c.pinnedTypes.putIfAbsent(type.getFullyQualifiedName(), "is used as a `Class` value in `" + source.getSourcePath() + "`");
                    }
                    return super.visitFieldAccess(fieldAccess, c);
                }

                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Candidate c) {
                    passesAsObject(method.getMethodType(), method.getArguments(), c);
                    return super.visitMethodInvocation(method, c);
                }

                @Override
                public J.NewClass visitNewClass(J.NewClass newClass, Candidate c) {
                    passesAsObject(newClass.getConstructorType(), newClass.getArguments(), c);
                    return super.visitNewClass(newClass, c);
                }

                private void passesAsObject(JavaType.@Nullable Method method, List<Expression> arguments, Candidate c) {
                    if (method == null) {
                        return;
                    }
                    List<JavaType> parameters = method.getParameterTypes();
                    for (int i = 0; i < arguments.size() && !parameters.isEmpty(); i++) {
                        JavaType parameter = parameters.get(Math.min(i, parameters.size() - 1));
                        if (parameter instanceof JavaType.Array && i >= parameters.size() - 1) {
                            parameter = ((JavaType.Array) parameter).getElemType();
                        }
                        JavaType.FullyQualified argument = TypeUtils.asFullyQualified(arguments.get(i).getType());
                        if (TypeUtils.isOfClassType(parameter, "java.lang.Object") && argument != null && isJodaTime(argument)) {
                            c.passedAsObject.putIfAbsent(argument.getFullyQualifiedName(), argument);
                        }
                    }
                }

                /// A class literal passed where the method declares `Class<T>` only tells the method which type to work
                /// with, like a Mockito matcher or a JPA query does, rather than looking up values by their runtime type.
                private boolean isTypeToken(J.FieldAccess classLiteral) {
                    Object parent = getCursor().getParentTreeCursor().getValue();
                    if (!(parent instanceof J.MethodInvocation) || ((J.MethodInvocation) parent).getMethodType() == null) {
                        return false;
                    }
                    J.MethodInvocation invocation = (J.MethodInvocation) parent;
                    int index = invocation.getArguments().indexOf(classLiteral);
                    JavaType.Method declared = declaration(invocation.getMethodType(), invocation.getMethodType().getDeclaringType(), 0);
                    if (declared == null || index < 0 || index >= declared.getParameterTypes().size()) {
                        return false;
                    }
                    JavaType parameter = declared.getParameterTypes().get(index);
                    if (!(parameter instanceof JavaType.Parameterized) || !TypeUtils.isOfClassType(parameter, "java.lang.Class") ||
                        ((JavaType.Parameterized) parameter).getTypeParameters().size() != 1) {
                        return false;
                    }
                    JavaType typeParameter = ((JavaType.Parameterized) parameter).getTypeParameters().get(0);
                    return typeParameter instanceof JavaType.GenericTypeVariable &&
                           !"?".equals(((JavaType.GenericTypeVariable) typeParameter).getName());
                }

                @Override
                public J.Annotation visitAnnotation(J.Annotation annotation, Candidate c) {
                    if (annotation.getArguments() != null) {
                        boolean hibernateType = TypeUtils.isOfClassType(annotation.getType(), "org.hibernate.annotations.Type");
                        for (Expression argument : annotation.getArguments()) {
                            Expression value = argument instanceof J.Assignment ? ((J.Assignment) argument).getAssignment() : argument;
                            if (value instanceof J.Literal && ((J.Literal) value).getValue() instanceof String) {
                                String text = (String) ((J.Literal) value).getValue();
                                if (hibernateType) {
                                    c.typeAliases.add(text);
                                    JavaType.FullyQualified aliased = TypeUtils.asFullyQualified(annotatedType());
                                    if (aliased != null && isJodaTime(aliased)) {
                                        c.aliasedTypes.putIfAbsent(text, aliased.getFullyQualifiedName());
                                    }
                                }
                                if (text.contains("org.joda.time") || text.contains("dateandtime.joda")) {
                                    // the framework maps the annotated member with a Joda-Time specific type, which every other
                                    // member of this type relies on as well
                                    JavaType.FullyQualified mapped = TypeUtils.asFullyQualified(annotatedType());
                                    if (mapped != null && isJodaTime(mapped)) {
                                        c.pinnedTypes.putIfAbsent(mapped.getFullyQualifiedName(), "is mapped with `" + text + "` in `" + source.getSourcePath() + "`");
                                    }
                                }
                            }
                        }
                    }
                    return super.visitAnnotation(annotation, c);
                }

                private @Nullable JavaType annotatedType() {
                    Object annotated = getCursor().getParentTreeCursor().getValue();
                    if (annotated instanceof J.VariableDeclarations) {
                        return ((J.VariableDeclarations) annotated).getType();
                    }
                    if (annotated instanceof J.MethodDeclaration && ((J.MethodDeclaration) annotated).getMethodType() != null) {
                        return ((J.MethodDeclaration) annotated).getMethodType().getReturnType();
                    }
                    return null;
                }

                @Override
                public J.Literal visitLiteral(J.Literal literal, Candidate c) {
                    if (literal.getValue() instanceof String) {
                        String value = (String) literal.getValue();
                        if (value.contains("org.joda.time") || value.contains("dateandtime.joda")) {
                            c.problems.add("Names Joda-Time in the string " + literal.getValueSource());
                        }
                    }
                    return literal;
                }
            }.visit(source, candidate);
        } else {
            candidate.problems.add("Only Java sources are migrated");
        }
    }

    private static void share(JavaType.Method method, Candidate candidate, Set<String> declaredTypes) {
        if (!mentionsJodaTime(method, 0)) {
            return;
        }
        String owner = method.getDeclaringType().getFullyQualifiedName();
        if (!isShareable(owner, declaredTypes) || candidate.sharedTypes.get(owner) != null) {
            return;
        }
        JavaType.Method declared = declaration(method, method.getDeclaringType(), 0);
        boolean declaredWithJodaTime = declared == null || mentionsJodaTime(declared, 0);
        candidate.sharedTypes.put(owner, declaredWithJodaTime ? owner + "#" + method.getName() : null);
    }

    /// Joda-Time's ISO parsers accept dates without a time and date times without an offset, where `java.time`'s
    /// `parse(CharSequence)` methods are strict, so only string literals that the `java.time` type accepts are safe.
    private static boolean parsesTheSame(JavaType.FullyQualified jodaType, Expression text) {
        String javaTimeType = JodaTimeTypes.javaTimeType(jodaType);
        Class<?> javaTimeClass = javaTimeType == null ? null : loadClass(javaTimeType);
        if (javaTimeClass == null || !(text instanceof J.Literal) || !(((J.Literal) text).getValue() instanceof String)) {
            return false;
        }
        try {
            javaTimeClass.getMethod("parse", CharSequence.class).invoke(null, ((J.Literal) text).getValue());
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /// Hibernate type aliases of Joda-Time user types, in any attribute order, and Joda-Time types that the
    /// configuration names, which are looked up by name at runtime.
    private static void scanXml(SourceFile xml, Accumulator acc) {
        String text = XML_COMMENT_OR_CDATA.matcher(xml.printAll()).replaceAll("");
        Matcher typedef = XML_TYPEDEF.matcher(text);
        while (typedef.find()) {
            Matcher name = XML_TYPEDEF_NAME.matcher(typedef.group(1));
            Matcher typeClass = XML_TYPEDEF_CLASS.matcher(typedef.group(1));
            if (name.find() && typeClass.find() && typeClass.group(1).toLowerCase(Locale.ROOT).contains("joda")) {
                acc.jodaTypeAliases.put(name.group(1), typeClass.group(1));
                acc.blocked = null;
            }
        }
        Set<String> aliasReferences = new HashSet<>();
        Matcher property = XML_MAPPED_PROPERTY_TYPE.matcher(text);
        while (property.find()) {
            aliasReferences.add(property.group(1));
        }
        if (!aliasReferences.isEmpty() || acc.xmlTypeAliasReferences.containsKey(xml.getSourcePath())) {
            acc.xmlTypeAliasReferences.put(xml.getSourcePath(), aliasReferences);
            acc.blocked = null;
        }
        Map<String, String> named = new HashMap<>();
        // build and static analysis descriptors name Joda-Time types without anything looking them up at runtime
        if (!XML_NOT_CONFIGURATION.matcher(xml.getSourcePath().toString().replace('\\', '/')).find()) {
            Matcher reference = XML_JODA_TIME_TYPE_REFERENCE.matcher(text);
            while (reference.find()) {
                String jodaType = reference.group(2) != null ? reference.group(2) : reference.group(4);
                named.putIfAbsent(jodaType, "is named in `" + xml.getSourcePath() + "` as `" + reference.group().trim() + "`");
            }
        }
        if (!named.isEmpty() || acc.xmlPinnedTypes.containsKey(xml.getSourcePath())) {
            acc.xmlPinnedTypes.put(xml.getSourcePath(), named);
            acc.blocked = null;
        }
    }

    /// The Joda-Time type that a Jadira user type such as `PersistentDateTime` or `PersistentInstantAsMillisLong` persists.
    private static @Nullable String jodaTimeTypeOfUserType(@Nullable String userType) {
        Matcher persisted = JADIRA_PERSISTED_TYPE.matcher(userType == null ? "" : userType);
        if (persisted.find()) {
            for (String simpleName : JODA_TIME_SIMPLE_NAMES) {
                if (persisted.group(1).startsWith(simpleName)) {
                    return "org.joda.time." + simpleName;
                }
            }
        }
        return null;
    }

    /// An inherited implementation only satisfies a supertype method while both agree on Joda-Time or `java.time`. So a
    /// class is tied to a supertype method that mentions Joda-Time when it overrides it, when it implements it with a
    /// method inherited from another supertype, or when it leaves it abstract for its own subclasses to implement.
    private static void shareSupertypes(JavaType.FullyQualified type, Candidate candidate, Set<String> declaredTypes) {
        List<JavaType.FullyQualified> ancestors = new ArrayList<>();
        collectSupertypes(type, ancestors, new HashSet<>(), 0);
        for (JavaType.FullyQualified ancestor : ancestors) {
            for (JavaType.Method method : ancestor.getMethods()) {
                if (!mentionsJodaTime(method, 0)) {
                    continue;
                }
                boolean abstractMethod = method.hasFlags(Flag.Abstract) || ancestor.getKind() == JavaType.FullyQualified.Kind.Interface;
                if (declares(type, method)) {
                    share(method, candidate, declaredTypes);
                } else if (abstractMethod) {
                    share(method, candidate, declaredTypes);
                    for (JavaType.FullyQualified provider : ancestors) {
                        if (provider != ancestor && provider.getKind() != JavaType.FullyQualified.Kind.Interface) {
                            for (JavaType.Method implementation : provider.getMethods()) {
                                if (!implementation.hasFlags(Flag.Abstract) && implementation.getName().equals(method.getName()) &&
                                    implementation.getParameterTypes().size() == method.getParameterTypes().size()) {
                                    share(implementation, candidate, declaredTypes);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void collectSupertypes(JavaType.FullyQualified type, List<JavaType.FullyQualified> ancestors, Set<String> seen, int depth) {
        List<JavaType.FullyQualified> supertypes = new ArrayList<>(type.getInterfaces());
        if (type.getSupertype() != null) {
            supertypes.add(type.getSupertype());
        }
        for (JavaType.FullyQualified supertype : supertypes) {
            String name = supertype.getFullyQualifiedName();
            if (depth <= 8 && !isJodaTime(supertype) && !name.startsWith("java.") && seen.add(name)) {
                ancestors.add(supertype);
                collectSupertypes(supertype, ancestors, seen, depth + 1);
            }
        }
    }

    private static boolean declares(JavaType.FullyQualified type, JavaType.Method method) {
        for (JavaType.Method declared : type.getMethods()) {
            if (declared.getName().equals(method.getName()) && declared.getParameterTypes().size() == method.getParameterTypes().size()) {
                return true;
            }
        }
        return false;
    }

    private static void collectTypeDefs(JavaSourceFile source, Map<String, String> jodaTypeAliases) {
        new JavaIsoVisitor<Map<String, String>>() {
            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, Map<String, String> aliases) {
                if (TypeUtils.isOfClassType(annotation.getType(), "org.hibernate.annotations.TypeDef") && annotation.getArguments() != null) {
                    String name = null;
                    String userType = null;
                    for (Expression argument : annotation.getArguments()) {
                        if (argument instanceof J.Assignment) {
                            J.Assignment assignment = (J.Assignment) argument;
                            String key = assignment.getVariable() instanceof J.Identifier ? ((J.Identifier) assignment.getVariable()).getSimpleName() : "";
                            if ("name".equals(key) && assignment.getAssignment() instanceof J.Literal) {
                                name = String.valueOf(((J.Literal) assignment.getAssignment()).getValue());
                            } else if ("typeClass".equals(key) &&
                                       String.valueOf(assignment.getAssignment().getType()).toLowerCase(Locale.ROOT).contains("joda")) {
                                userType = String.valueOf(assignment.getAssignment().getType());
                            }
                        }
                    }
                    if (name != null && userType != null) {
                        aliases.put(name, userType);
                    }
                }
                return super.visitAnnotation(annotation, aliases);
            }
        }.visit(source, jodaTypeAliases);
    }

    /// The declaration of an invoked method, also when it is inherited, as `Mockito.any` is from `ArgumentMatchers`.
    private static JavaType.@Nullable Method declaration(JavaType.Method method, JavaType.@Nullable FullyQualified type, int depth) {
        if (type == null || depth > 8) {
            return null;
        }
        for (JavaType.Method declared : type.getMethods()) {
            if (declared.getName().equals(method.getName()) && declared.getParameterTypes().size() == method.getParameterTypes().size()) {
                return declared;
            }
        }
        JavaType.Method inherited = declaration(method, type.getSupertype(), depth + 1);
        for (JavaType.FullyQualified anInterface : type.getInterfaces()) {
            if (inherited == null) {
                inherited = declaration(method, anInterface, depth + 1);
            }
        }
        return inherited;
    }

    private static boolean isShareable(String owner, Set<String> declaredTypes) {
        if (owner.startsWith("org.joda.time.") || owner.startsWith("java.")) {
            return false;
        }
        for (String t = owner; ; t = t.substring(0, t.lastIndexOf('$'))) {
            if (declaredTypes.contains(t)) {
                return false;
            }
            if (t.lastIndexOf('$') < 0) {
                return true;
            }
        }
    }

    /// Checks the migrated source for anything that would not compile against `java.time`.
    private static class MigrationVerifier extends JavaIsoVisitor<ExecutionContext> {
        private final Candidate candidate;

        MigrationVerifier(Candidate candidate) {
            this.candidate = candidate;
        }

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            Map<String, JavaType.FullyQualified> jodaTypes = new TreeMap<>();
            for (JavaType type : cu.getTypesInUse().getTypesInUse()) {
                collectJodaTypes(type, jodaTypes, 0);
            }
            for (String jodaType : jodaTypes.keySet()) {
                candidate.problems.add("org.joda.time.DateTimeUtils".equals(jodaType) ?
                        "Uses `org.joda.time.DateTimeUtils`, a global clock that `java.time` deliberately does not have; inject a `java.time.Clock` instead" :
                        "Uses `" + jodaType + "`, which has no mapping to `java.time`");
            }
            return super.visitCompilationUnit(cu, ctx);
        }

        @Override
        public J.Import visitImport(J.Import _import, ExecutionContext ctx) {
            if (_import.isStatic() && !"*".equals(_import.getQualid().getSimpleName())) {
                Class<?> owner = javaTimeClass(_import.getTypeName());
                String member = _import.getQualid().getSimpleName();
                if (owner != null && field(owner, member) == null && Arrays.stream(owner.getMethods()).noneMatch(m -> m.getName().equals(member))) {
                    candidate.problems.add("`" + owner.getSimpleName() + "." + member + "` does not exist");
                }
            }
            return _import;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            if (candidate.jodaParses.contains(method.getId())) {
                candidate.problems.add("Calls Joda-Time's `" + method.getSimpleName() + "(" + describe(method.getArguments()) +
                        ")`, which accepts more than its `java.time` counterpart, with arguments that can not be checked");
            }
            JavaType.Method type = method.getMethodType();
            if (type != null) {
                boolean isStatic = type.hasFlags(Flag.Static);
                Class<?> receiver = javaTimeClass(method.getSelect() != null ?
                        method.getSelect().getType() : isStatic ? type.getDeclaringType() : null);
                if (receiver == null) {
                    checkArguments(type.getParameterTypes(), method.getArguments());
                } else if (!hasMethod(receiver, method.getSimpleName(), isStatic, method.getArguments(), type.getReturnType())) {
                    candidate.problems.add("Calls `" + receiver.getSimpleName() + "." + method.getSimpleName() + "(" +
                            describe(method.getArguments()) + ")`, which has no `java.time` equivalent");
                }
            }
            return super.visitMethodInvocation(method, ctx);
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            Class<?> created = javaTimeClass(newClass.getType());
            if (created != null) {
                candidate.problems.add("Calls `new " + created.getSimpleName() + "(" + describe(newClass.getArguments()) +
                        ")`, which has no `java.time` equivalent");
            }
            if (newClass.getConstructorType() != null) {
                checkArguments(newClass.getConstructorType().getParameterTypes(), newClass.getArguments());
            }
            return super.visitNewClass(newClass, ctx);
        }

        @Override
        public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
            Class<?> owner = javaTimeClass(memberRef.getContaining().getType());
            String name = memberRef.getReference().getSimpleName();
            if (owner != null && !"new".equals(name) && getCursor().firstEnclosing(Javadoc.class) == null &&
                    field(owner, name) == null && Arrays.stream(owner.getMethods()).noneMatch(m -> m.getName().equals(name))) {
                candidate.problems.add("References `" + owner.getSimpleName() + "::" + name + "`, which has no `java.time` equivalent");
            }
            return super.visitMemberReference(memberRef, ctx);
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
            JavaType.Variable fieldType = identifier.getFieldType();
            if (fieldType != null) {
                Class<?> owner = javaTimeClass(fieldType.getOwner());
                if (owner != null && !"class".equals(identifier.getSimpleName()) && field(owner, identifier.getSimpleName()) == null) {
                    candidate.problems.add("`" + owner.getSimpleName() + "." + identifier.getSimpleName() + "` does not exist");
                }
            }
            return super.visitIdentifier(identifier, ctx);
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
            if (variable.getInitializer() != null) {
                checkAssignable(variable.getType(), variable.getInitializer());
            }
            return super.visitVariable(variable, ctx);
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
            checkAssignable(assignment.getVariable().getType(), assignment.getAssignment());
            return super.visitAssignment(assignment, ctx);
        }

        @Override
        public J.Return visitReturn(J.Return _return, ExecutionContext ctx) {
            Object enclosing = getCursor().dropParentUntil(p -> p instanceof J.MethodDeclaration ||
                    p instanceof J.Lambda || p instanceof J.ClassDeclaration || p == Cursor.ROOT_VALUE).getValue();
            if (_return.getExpression() != null && enclosing instanceof J.MethodDeclaration &&
                    ((J.MethodDeclaration) enclosing).getMethodType() != null) {
                checkAssignable(((J.MethodDeclaration) enclosing).getMethodType().getReturnType(), _return.getExpression());
            }
            return super.visitReturn(_return, ctx);
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            Set<String> signatures = new HashSet<>();
            for (Statement statement : classDecl.getBody().getStatements()) {
                if (statement instanceof J.MethodDeclaration && ((J.MethodDeclaration) statement).getMethodType() != null) {
                    JavaType.Method type = ((J.MethodDeclaration) statement).getMethodType();
                    StringJoiner signature = new StringJoiner(", ", type.getName() + "(", ")");
                    for (JavaType parameter : type.getParameterTypes()) {
                        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(parameter);
                        signature.add(fq == null ? String.valueOf(parameter) : fq.getFullyQualifiedName());
                    }
                    if (!signatures.add(signature.toString())) {
                        candidate.problems.add("Would declare `" + signature + "` twice in `" + classDecl.getSimpleName() + "`");
                    }
                }
            }
            return super.visitClassDeclaration(classDecl, ctx);
        }

        private void checkArguments(List<JavaType> parameterTypes, List<Expression> arguments) {
            if (parameterTypes.size() == arguments.size()) {
                for (int i = 0; i < arguments.size(); i++) {
                    checkAssignable(parameterTypes.get(i), arguments.get(i));
                }
            }
        }

        private void checkAssignable(@Nullable JavaType to, Expression expression) {
            Class<?> toClass = javaTimeClass(to);
            Class<?> fromClass = javaTimeClass(expression.getType());
            if (toClass != null && fromClass != null && !toClass.isAssignableFrom(fromClass)) {
                candidate.problems.add("Uses a `" + fromClass.getSimpleName() + "` where a `" + toClass.getSimpleName() + "` is expected");
            }
        }

        private static String describe(List<Expression> arguments) {
            StringJoiner description = new StringJoiner(", ");
            for (Expression argument : arguments) {
                if (!(argument instanceof J.Empty)) {
                    JavaType type = argument.getType();
                    JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
                    description.add(fq != null ? fq.getClassName() : type instanceof JavaType.Primitive ?
                            ((JavaType.Primitive) type).getKeyword() : "?");
                }
            }
            return description.toString();
        }
    }

    private static @Nullable Class<?> javaTimeClass(@Nullable JavaType type) {
        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
        return fq == null ? null : javaTimeClass(fq.getFullyQualifiedName());
    }

    private static @Nullable Class<?> javaTimeClass(String fullyQualifiedName) {
        if (!fullyQualifiedName.startsWith("java.time.") && !fullyQualifiedName.startsWith("org.threeten.extra.")) {
            return null;
        }
        return loadClass(fullyQualifiedName);
    }

    private static @Nullable Class<?> loadClass(String fullyQualifiedName) {
        return CLASSES.computeIfAbsent(fullyQualifiedName, name -> {
            try {
                return Optional.of(Class.forName(name, false, JodaTimeToJavaTime.class.getClassLoader()));
            } catch (ClassNotFoundException | LinkageError e) {
                return Optional.empty();
            }
        }).orElse(null);
    }

    private static @Nullable Field field(Class<?> owner, String name) {
        try {
            return owner.getField(name);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static boolean hasMethod(Class<?> receiver, String name, boolean isStatic, List<Expression> arguments,
                                     @Nullable JavaType returnType) {
        List<Expression> args = arguments.size() == 1 && arguments.get(0) instanceof J.Empty ? emptyList() : arguments;
        for (Method method : receiver.getMethods()) {
            if (!method.getName().equals(name) || Modifier.isStatic(method.getModifiers()) != isStatic ||
                    method.getParameterCount() != args.size()) {
                continue;
            }
            boolean argumentsFit = true;
            for (int i = 0; i < args.size() && argumentsFit; i++) {
                argumentsFit = accepts(method.getParameterTypes()[i], args.get(i).getType());
            }
            if (argumentsFit && returns(method.getReturnType(), returnType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean accepts(Class<?> parameter, @Nullable JavaType argument) {
        if (argument == JavaType.Primitive.Null) {
            return !parameter.isPrimitive();
        }
        Class<?> argumentClass = argument instanceof JavaType.Primitive ?
                primitiveClass((JavaType.Primitive) argument) : jdkClass(argument);
        return argumentClass == null || isConvertible(argumentClass, parameter);
    }

    private static boolean returns(Class<?> actual, @Nullable JavaType expected) {
        if (expected instanceof JavaType.Primitive) {
            Class<?> expectedClass = primitiveClass((JavaType.Primitive) expected);
            return expectedClass == null || isConvertible(actual, expectedClass);
        }
        if (isJodaTime(expected)) {
            return false;
        }
        Class<?> expectedClass = jdkClass(expected);
        return expectedClass == null || isConvertible(actual, expectedClass);
    }

    private static @Nullable Class<?> jdkClass(@Nullable JavaType type) {
        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
        return fq != null && (fq.getFullyQualifiedName().startsWith("java.") || fq.getFullyQualifiedName().startsWith("org.threeten.extra.")) ?
                loadClass(fq.getFullyQualifiedName()) : null;
    }

    private static @Nullable Class<?> primitiveClass(JavaType.Primitive primitive) {
        switch (primitive) {
            case Boolean:
                return boolean.class;
            case Byte:
                return byte.class;
            case Char:
                return char.class;
            case Double:
                return double.class;
            case Float:
                return float.class;
            case Int:
                return int.class;
            case Long:
                return long.class;
            case Short:
                return short.class;
            case String:
                return String.class;
            case Void:
                return void.class;
            default:
                return null;
        }
    }

    private static final List<Class<?>> WIDENING = Arrays.asList(
            byte.class, short.class, char.class, int.class, long.class, float.class, double.class);

    private static final Map<Class<?>, Class<?>> BOXES = new HashMap<>();

    static {
        BOXES.put(boolean.class, Boolean.class);
        BOXES.put(byte.class, Byte.class);
        BOXES.put(char.class, Character.class);
        BOXES.put(double.class, Double.class);
        BOXES.put(float.class, Float.class);
        BOXES.put(int.class, Integer.class);
        BOXES.put(long.class, Long.class);
        BOXES.put(short.class, Short.class);
    }

    private static boolean isConvertible(Class<?> from, Class<?> to) {
        if (to.isAssignableFrom(from)) {
            return true;
        }
        if (from.isPrimitive() && !to.isPrimitive()) {
            return BOXES.containsKey(from) && to.isAssignableFrom(BOXES.get(from));
        }
        if (!from.isPrimitive() && to.isPrimitive()) {
            for (Map.Entry<Class<?>, Class<?>> box : BOXES.entrySet()) {
                if (box.getValue().equals(from)) {
                    return isConvertible(box.getKey(), to);
                }
            }
            return false;
        }
        int fromIndex = WIDENING.indexOf(from);
        int toIndex = WIDENING.indexOf(to);
        return fromIndex >= 0 && toIndex > fromIndex && !(from == char.class && to == short.class) &&
                !(from == short.class && to == char.class) && !(from == byte.class && to == char.class);
    }
}
