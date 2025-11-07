package org.openrewrite.java.joda.time;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

public class BaseDateTimeMethods extends Recipe {

    private static final String JAVA_LOCAL_DATE = "java.time.LocalDate";
    private static final String JAVA_ZONE_ID = "java.time.ZoneId";
    private static final String JAVA_ZONE_OFFSET = "java.time.ZoneOffset";
    private static final MethodMatcher baseDateTimeGetMills = new MethodMatcher("org.joda.time.base.BaseDateTime getMillis()");
    private static final JavaTemplate javaTimeReplacement = JavaTemplate
            .builder("LocalDate.now().atStartOfDay().toInstant(ZoneOffset.of(ZoneId.systemDefault().getId())).toEpochMilli()")
            .imports(JAVA_LOCAL_DATE, JAVA_ZONE_ID, JAVA_ZONE_OFFSET)
            .build();

    @Override
    public String getDisplayName() {
        return "Replace `BaseDateTime getMillis()` with Java Time API";
    }

    @Override
    public String getDescription() {
        return "Replace `BaseDateTime getMillis()` with Java Time API equivalent with" +
                "```java" +
                "LocalDate.now()" +
                "   .atStartOfDay()" +
                "   .toInstant(ZoneOffset.of(ZoneId.systemDefault().getId()))" +
                "   .toEpochMilli()" +
                "```" +
                "to migrate away from Joda Time.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(baseDateTimeGetMills),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        if (!baseDateTimeGetMills.matches(method)) {
                            return super.visitMethodInvocation(method, ctx);
                        }

                        maybeAddImport(JAVA_LOCAL_DATE);
                        maybeAddImport(JAVA_ZONE_ID);
                        maybeAddImport(JAVA_ZONE_OFFSET);

                        J.MethodInvocation methodSelect = (J.MethodInvocation) method.getSelect();
                        if (methodSelect != null && methodSelect.getMethodType() != null) {
                            maybeRemoveImport(methodSelect.getMethodType().getDeclaringType());
                        }

                        return javaTimeReplacement.apply(getCursor(), method.getCoordinates().replace());
                    }
                }
        );
    }
}
