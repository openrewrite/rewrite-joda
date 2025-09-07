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

import lombok.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaFieldTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.JavadocVisitor;
import org.openrewrite.java.joda.time.templates.AllTemplates;
import org.openrewrite.java.joda.time.templates.MethodTemplate;
import org.openrewrite.java.joda.time.templates.TimeClassMap;
import org.openrewrite.java.joda.time.templates.VarTemplates;
import org.openrewrite.java.tree.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.openrewrite.java.joda.time.templates.TimeClassNames.*;

class JodaTimeVisitor extends JavaVisitor<ExecutionContext> {

    public JodaTimeVisitor() {
        super();
    }

    @Override
    protected JavadocVisitor<ExecutionContext> getJavadocVisitor() {
        return new JavadocVisitor<>(this) {
            /**
             * Do not visit the method referenced from the Javadoc, may cause recipe to fail.
             */
            @Override
            public Javadoc visitReference(Javadoc.Reference reference, ExecutionContext ctx) {
                return reference;
            }
        };
    }

    @Override
    public @NonNull J visitCompilationUnit(@NonNull J.CompilationUnit cu, @NonNull ExecutionContext ctx) {
        J j = super.visitCompilationUnit(cu, ctx);
        if (j != cu) {
            maybeRemoveImport(JODA_DATE_TIME);
            maybeRemoveImport(JODA_DATE_TIME_ZONE);
            maybeRemoveImport(JODA_TIME_FORMAT);
            maybeRemoveImport(JODA_DURATION);
            maybeRemoveImport(JODA_ABSTRACT_INSTANT);
            maybeRemoveImport(JODA_INSTANT);
            maybeRemoveImport(JODA_INTERVAL);
            maybeRemoveImport(JODA_TIME_FORMATTER);
            maybeRemoveImport(JODA_LOCAL_DATE);
            maybeRemoveImport(JODA_LOCAL_TIME);

            maybeAddImport(JAVA_DATE_TIME);
            maybeAddImport(JAVA_ZONE_OFFSET);
            maybeAddImport(JAVA_ZONE_ID);
            maybeAddImport(JAVA_INSTANT);
            maybeAddImport(JAVA_TIME_FORMATTER);
            maybeAddImport(JAVA_TIME_FORMAT_STYLE);
            maybeAddImport(JAVA_DURATION);
            maybeAddImport(JAVA_LOCAL_DATE);
            maybeAddImport(JAVA_LOCAL_TIME);
            maybeAddImport(JAVA_TEMPORAL_ISO_FIELDS);
            maybeAddImport(JAVA_CHRONO_FIELD);
            maybeAddImport(JAVA_UTIL_DATE);
            maybeAddImport(THREE_TEN_EXTRA_INTERVAL);
        }
        return j;
    }

    @Override
    public @NonNull J visitMethodDeclaration(@NonNull J.MethodDeclaration method, @NonNull ExecutionContext ctx) {
        J.MethodDeclaration m = (J.MethodDeclaration) super.visitMethodDeclaration(method, ctx);
        if (m.getReturnTypeExpression() == null || !m.getType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            return m;
        }

        JavaType.Class returnType = TimeClassMap.getJavaTimeType(((JavaType.Class) m.getType()).getFullyQualifiedName());
        J.Identifier returnExpr = TypeTree.build(returnType.getClassName()).withType(returnType).withPrefix(Space.format(" "));

        JavaType.Method methodType = m.getMethodType()
                .withReturnType(returnType)
                .withParameterTypes(m.getMethodType().getParameterTypes().stream().map(p -> visitTypeParameter(p, ctx)).collect(Collectors.toList()));

        return m
                .withReturnTypeExpression(returnExpr)
                .withMethodType(methodType);
    }
    JavaType visitTypeParameter(JavaType parameter, @NonNull ExecutionContext ctx){
        if (parameter instanceof JavaType.Class) {return visitClassTypeParameter((JavaType.Class)parameter, ctx);}
        if (parameter instanceof JavaType.Array) {return ((JavaType.Array)parameter).withElemType(visitClassTypeParameter((JavaType.Class)((JavaType.Array) parameter).getElemType(), ctx));}
        return parameter;
    }
    JavaType visitClassTypeParameter(JavaType.Class classParameter, @NonNull ExecutionContext ctx) {
        if (!classParameter.isAssignableFrom(JODA_CLASS_PATTERN)) {
            return classParameter;
        }
        return TimeClassMap.getJavaTimeType(classParameter.getFullyQualifiedName());
    }

    @Override
    public @NonNull J visitVariableDeclarations(@NonNull J.VariableDeclarations multiVariable, @NonNull ExecutionContext ctx) {
        J.VariableDeclarations mv = (J.VariableDeclarations) super.visitVariableDeclarations(multiVariable, ctx);

        if (multiVariable.getTypeExpression() == null || !mv.getType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            return mv;
        }

        String fullyQualifiedName = ((JavaType.Class) mv.getType()).getFullyQualifiedName();
        JavaType.Class javaTimeType = TimeClassMap.getJavaTimeType(fullyQualifiedName);
        String javaTimeShortName = TimeClassMap.getJavaTimeShortName(fullyQualifiedName);

        if(javaTimeType == null) {
            System.out.println("Joda type is found but mapping is missing: " + fullyQualifiedName);
            return mv;
        }

        J.Identifier typeExpression = (J.Identifier) mv.getTypeExpression();

        if(javaTimeShortName != null){
            typeExpression = typeExpression.withSimpleName(javaTimeShortName);
        }

        return autoFormat(mv.withTypeExpression(typeExpression.withType(javaTimeType)), ctx);
    }

    @Override
    public @NonNull J visitVariable(@NonNull J.VariableDeclarations.NamedVariable variable, @NonNull ExecutionContext ctx) {
        J.VariableDeclarations.NamedVariable v = (J.VariableDeclarations.NamedVariable) super.visitVariable(variable, ctx);
        if (v.getType() instanceof JavaType.Array && ((JavaType.Array)v.getType()).getElemType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            JavaType.Array type = (JavaType.Array) v.getType();
            JavaType.Class elemType = (JavaType.Class) type.getElemType();
            return v.withType(type.withElemType(TimeClassMap.getJavaTimeType(elemType.getFullyQualifiedName())));
        }
        if (v.getType() instanceof JavaType.Class && v.getType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            return v.withType(TimeClassMap.getJavaTimeType(((JavaType.Class) variable.getType()).getFullyQualifiedName()));
        }

        return v;
    }

    @Override
    public @NonNull J visitAssignment(@NonNull J.Assignment assignment, @NonNull ExecutionContext ctx) {
        J.Assignment a = (J.Assignment) super.visitAssignment(assignment, ctx);
        if (!a.getType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            return a;
        }
        if (!(a.getVariable() instanceof J.Identifier)) {
            return assignment;
        }
        J.Identifier varName = (J.Identifier) a.getVariable();
        return VarTemplates.getTemplate(assignment).<J>map(t -> t.apply(
                updateCursor(a),
                a.getCoordinates().replace(),
                varName,
                a.getAssignment())).orElse(assignment);
    }

    @Override
    public @NonNull J visitNewClass(@NonNull J.NewClass newClass, @NonNull ExecutionContext ctx) {
        MethodCall updated = (MethodCall) super.visitNewClass(newClass, ctx);
        if (hasJodaType(updated.getArguments())) {
            return newClass;
        }
        return migrateMethodCall(newClass, updated);
    }


    @Override
    public @NonNull J visitMethodInvocation(@NonNull J.MethodInvocation method, @NonNull ExecutionContext ctx) {
        J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

        // internal method with Joda class as return type
        if (method.getMethodType() != null &&
                !method.getMethodType().getDeclaringType().isAssignableFrom(JODA_CLASS_PATTERN) &&
                method.getType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            return migrateNonJodaMethod(method, m);
        }

        if (hasJodaType(m.getArguments()) || isJodaVarRef(m.getSelect())) {
            return method;
        }
        return migrateMethodCall(method, m);
    }

    @Override
    public @NonNull J visitFieldAccess(@NonNull J.FieldAccess fieldAccess, @NonNull ExecutionContext ctx) {
        J.FieldAccess f = (J.FieldAccess) super.visitFieldAccess(fieldAccess, ctx);
        if (!isJodaVarRef(fieldAccess)) {
            return f;
        }

        String fullyQualifiedName = ((JavaType.Class) f.getType()).getFullyQualifiedName();
        JavaType.Class javaTimeType = TimeClassMap.getJavaTimeType(fullyQualifiedName);
        String javaTimeShortName = TimeClassMap.getJavaTimeShortName(fullyQualifiedName);
        J.Identifier fieldName = f.getName();

        JavaFieldTemplate fieldTemplate = AllTemplates.getFieldTemplate(f);
        if(fieldTemplate != null) {
            javaTimeShortName = Arrays.stream(fieldTemplate.getType().split("\\.")).toList().getLast();
            javaTimeType = new JavaType.Class(null, 0, fieldTemplate.getType(), JavaType.FullyQualified.Kind.Class, null, JavaType.ShallowClass.build("java.lang.Object"),
                    null, null, null, null, null);
            fieldName = fieldName.withSimpleName(fieldTemplate.getName());
        }

        Expression target = f.getTarget();
        if(target instanceof J.Identifier) {
            if (javaTimeShortName != null) {
                target = ((J.Identifier) target).withSimpleName(javaTimeShortName).withType(javaTimeType);
            }
        }

        return f.withTarget(target).withType(javaTimeType).withName(fieldName);
    }

    @Override
    public @NonNull J visitIdentifier(@NonNull J.Identifier ident, @NonNull ExecutionContext ctx) {
        J.Identifier i = (J.Identifier) super.visitIdentifier(ident, ctx);
        //i.getType().isAssignableFrom(JODA_CLASS_PATTERN)
        if (!isJodaVarRef(i)) {
            return i;
        }
        JavaType type = i.getType();
        if(type instanceof JavaType.Array) {return visitArrayIdentifier(i, ctx);}
        if(type instanceof JavaType.Class) {return visitClassIdentifier(i, ctx);}

        return i;
    }
    private @NonNull J visitArrayIdentifier(J.Identifier arrayIdentifier, @NonNull ExecutionContext ctx) {
        JavaType.Array at = (JavaType.Array)arrayIdentifier.getType();
        JavaType.Array javaTimeType = at.withElemType(TimeClassMap.getJavaTimeType(((JavaType.Class)at.getElemType()).getFullyQualifiedName()));

        return arrayIdentifier.withType(javaTimeType)
                .withFieldType(arrayIdentifier.getFieldType().withType(javaTimeType));
    }
    private @NonNull J visitClassIdentifier(J.Identifier classIdentifier, @NonNull ExecutionContext ctx) {
        JavaType.Class javaTimeType = TimeClassMap.getJavaTimeType(((JavaType.Class)classIdentifier.getType()).getFullyQualifiedName());

        return classIdentifier.withType(javaTimeType)
                .withFieldType(classIdentifier.getFieldType().withType(javaTimeType));
    }

    @Override
    public J visitArrayAccess(J.ArrayAccess arrayAccess, @NonNull ExecutionContext ctx){
        J.ArrayAccess a = (J.ArrayAccess) super.visitArrayAccess(arrayAccess, ctx);
        if (!a.getType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            return a;
        }
        return a.withType(TimeClassMap.getJavaTimeType(((JavaType.Class)a.getType()).getFullyQualifiedName()));
    }

    private J migrateMethodCall(MethodCall original, MethodCall updated) {
        if (original.getMethodType() == null || !original.getMethodType().getDeclaringType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            return updated; // not a joda type, no need to migrate
        }
        MethodTemplate template = AllTemplates.getTemplate(original);
        if (template == null) {
            System.out.println("Joda usage is found but mapping is missing: " + original);
            return original; // unhandled case
        }
        if (template.getTemplate().getCode().equals(JODA_MULTIPLE_MAPPING_POSSIBLE)) {
            System.out.println(JODA_MULTIPLE_MAPPING_POSSIBLE + ": " + original);
            return original; // usage with no automated mapping
        }
        if (template.getTemplate().getCode().equals(JODA_NO_AUTOMATIC_MAPPING_POSSIBLE)) {
            System.out.println(JODA_NO_AUTOMATIC_MAPPING_POSSIBLE + ": " + original);
            return original; // usage with no automated mapping
        }
        Optional<J> maybeUpdated = applyTemplate(original, updated, template);
        if (!maybeUpdated.isPresent()) {
            System.out.println("Can not apply template: " + template + " to " + original);
            return original; // unhandled case
        }
        Expression updatedExpr = (Expression) maybeUpdated.get();
        if (!isArgument(original)) {
            return updatedExpr;
        }
        // this expression is an argument to a method call
        MethodCall parentMethod = getCursor().getParentTreeCursor().getValue();
        JavaType.Method parentMethodType = parentMethod.getMethodType();
        if (parentMethodType.getDeclaringType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            return updatedExpr;
        }
        int argPos = parentMethod.getArguments().indexOf(original);
        List<JavaType> parameterTypes = parentMethodType.getParameterTypes();
        int parameterTypesSize = parameterTypes.size();

        //try to process method with variable arguments
        if(argPos > parameterTypesSize)
        {
            //todo find better way to detect (...) in method arguments
            if (parameterTypes.get(parameterTypesSize - 1).toString().endsWith("[]")){
                return updatedExpr;
            }
            return original;
        }

        return updatedExpr;
    }

    private J.MethodInvocation migrateNonJodaMethod(J.MethodInvocation original, J.MethodInvocation updated) {
        JavaType.Class returnType = (JavaType.Class) updated.getMethodType().getReturnType();
        JavaType.Class updatedReturnType = TimeClassMap.getJavaTimeType(returnType.getFullyQualifiedName());
        if (updatedReturnType == null) {
            return original; // unhandled case
        }
        return updated.withMethodType(updated.getMethodType().withReturnType(updatedReturnType))
                .withName(updated.getName().withType(updatedReturnType));
    }

    private boolean hasJodaType(List<Expression> exprs) {
        for (Expression expr : exprs) {
            JavaType exprType = expr.getType();
            if (exprType != null && exprType.isAssignableFrom(JODA_CLASS_PATTERN)) {
                return true;
            }
        }
        return false;
    }

    private Optional<J> applyTemplate(MethodCall original, MethodCall updated, MethodTemplate template) {
        if (template.getMatcher().matches(original)) {
            Expression[] args = template.getTemplateArgsFunc().apply(updated);
            if (args.length == 0) {
                return Optional.of(template.getTemplate().apply(updateCursor(updated), updated.getCoordinates().replace()));
            }
            return Optional.of(template.getTemplate().apply(updateCursor(updated), updated.getCoordinates().replace(), (Object[]) args));
        }
        return Optional.empty(); // unhandled case
    }

    private boolean isJodaVarRef(@Nullable Expression expr) {
        if (expr == null || expr.getType() == null || !expr.getType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            return false;
        }
        if (expr instanceof J.FieldAccess) {
            return ((J.FieldAccess) expr).getName().getFieldType() != null;
        }
        if (expr instanceof J.Identifier) {
            return ((J.Identifier) expr).getFieldType() != null;
        }
        if (expr instanceof MethodCall) {
            return expr.getType().isAssignableFrom(JODA_CLASS_PATTERN);
        }
        return false;
    }

    private boolean isArgument(J expr) {
        if (!(getCursor().getParentTreeCursor().getValue() instanceof MethodCall)) {
            return false;
        }
        MethodCall methodCall = getCursor().getParentTreeCursor().getValue();
        return methodCall.getArguments().contains(expr);
    }
}
