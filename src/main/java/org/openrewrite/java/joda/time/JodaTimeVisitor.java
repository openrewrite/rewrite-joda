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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaFieldTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.JavadocVisitor;
import org.openrewrite.java.joda.time.templates.AllTemplates;
import org.openrewrite.java.joda.time.templates.MethodTemplate;
import org.openrewrite.java.joda.time.templates.TimeClassMap;
import org.openrewrite.java.table.TypeMappings;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Javadoc;
import org.openrewrite.java.tree.MethodCall;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.openrewrite.java.joda.time.templates.TimeClassNames.*;

//todo array processing logic can be simplified: type is changed by visitArrayAccess. the rest can be removed

class JodaTimeVisitor extends JavaVisitor<ExecutionContext> {
    private final MappingLogger logger;
    private final TimeClassMap timeClassMap;

    public JodaTimeVisitor(Recipe recipe) {
        logger = new MappingLogger(new TypeMappings(recipe));
        timeClassMap = new TimeClassMap(logger);
    }

    @Override
    protected JavadocVisitor<ExecutionContext> getJavadocVisitor() {
        return new JavadocVisitor<ExecutionContext>(this) {
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
        logger.setExecutionContext(ctx);

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
            maybeRemoveImport(JODA_DATE_TIME_MIDNIGHT);

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
        if (m.getReturnTypeExpression() == null) {
            return m;
        }
        if (!isJoda(m.getType())) {
            return m;
        }
        if (returnType == null) {
            return m;
        }
        if (returnSimpleName == null) {
            return m;
        }
        if (returnType == null) return m;

        String returnSimpleName = timeClassMap.getJavaTimeShortName(m.getType());
        if (returnSimpleName == null) return m;

        J.Identifier returnExpr = m.getReturnTypeExpression().withType(returnType);
        returnExpr = returnExpr
                    .withSimpleName(returnSimpleName);

        JavaType.Method methodType = m.getMethodType()
                .withReturnType(returnType)
                .withParameterTypes(m.getMethodType().getParameterTypes().stream().map(p -> visitTypeParameter(p, ctx)).collect(toList()));

        return m.withReturnTypeExpression(returnExpr)
                .withMethodType(methodType);
    }
    JavaType visitTypeParameter(JavaType parameter, @NonNull ExecutionContext ctx){
        if (parameter instanceof JavaType.Class) {return visitClassTypeParameter(parameter, ctx);}
        if (parameter instanceof JavaType.Array) {return ((JavaType.Array)parameter).withElemType(visitClassTypeParameter(((JavaType.Array) parameter).getElemType(), ctx));}
        return parameter;
    }
    JavaType visitClassTypeParameter(JavaType classParameter, @NonNull ExecutionContext ctx) {
        if (!isJoda(classParameter)) {
            return classParameter;
        }
        JavaType javaTimeType = timeClassMap.getJavaTimeType(classParameter);
        if (javaTimeType == null) return classParameter;

        return javaTimeType;
    }

    @Override
    public @NonNull J visitVariableDeclarations(@NonNull J.VariableDeclarations multiVariable, @NonNull ExecutionContext ctx) {
        J.VariableDeclarations mv = (J.VariableDeclarations) super.visitVariableDeclarations(multiVariable, ctx);
        if (multiVariable.getTypeExpression() == null) return mv;
        if (!isJoda(mv.getType())) return mv;

        JavaType javaTimeType = timeClassMap.getJavaTimeType(mv.getType());
        if (javaTimeType == null) return mv;

        String javaTimeShortName = timeClassMap.getJavaTimeShortName(mv.getType());
        if (javaTimeShortName == null) return mv;

        J.Identifier typeExpression = mv.getTypeExpression().withType(javaTimeType);
        typeExpression = typeExpression
                .withSimpleName(javaTimeShortName);

        return autoFormat(mv.withTypeExpression(typeExpression), ctx);
    }

    @Override
    public @NonNull J visitVariable(@NonNull J.VariableDeclarations.NamedVariable variable, @NonNull ExecutionContext ctx) {
        J.VariableDeclarations.NamedVariable v = (J.VariableDeclarations.NamedVariable) super.visitVariable(variable, ctx);
        JavaType variableType = extractVariableType(v);
        if (variableType == null) return v;
        if (!isJoda(variableType)) return v;

        JavaType javaTimeType = timeClassMap.getJavaTimeType(variableType);
        if (javaTimeType == null) return v;

        return v.withType(javaTimeType);
    }
    private JavaType extractVariableType(J.VariableDeclarations.NamedVariable variable) {
        JavaType jodaType = variable.getType();
        if (jodaType == null) return null;
        if (jodaType instanceof JavaType.Array) return ((JavaType.Array) jodaType).getElemType();
        if (jodaType instanceof JavaType.Class) return jodaType;
        return null;
    }

    @Override
    public @NonNull J visitAssignment(@NonNull J.Assignment assignment, @NonNull ExecutionContext ctx) {
        J.Assignment a = (J.Assignment) super.visitAssignment(assignment, ctx);
        if (!isJoda(a.getType())) return a;
        if (!(a.getVariable() instanceof J.Identifier)) return a; //todo what is this check covering?

        JavaType javaTimeType = timeClassMap.getJavaTimeType(a.getType());
        if (javaTimeType == null) return a;

        return a.withType(javaTimeType) ;
    }

    @Override
    public @NonNull J visitNewClass(@NonNull J.NewClass newClass, @NonNull ExecutionContext ctx) {
        MethodCall updated = (MethodCall) super.visitNewClass(newClass, ctx);

        return migrateMethodCall(newClass, updated, ctx);
    }

    @Override
    public @NonNull J visitMethodInvocation(@NonNull J.MethodInvocation method, @NonNull ExecutionContext ctx) {
        J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
        if (method.getMethodType() == null) return m;

        // internal method with Joda class as return type
        if (!isJoda(method.getMethodType().getDeclaringType()) &&
                isJoda(method.getType())) {
            return migrateNonJodaMethod(method, m);
        }

        return migrateMethodCall(method, m, ctx);
    }

    @Override
    public @NonNull J visitFieldAccess(@NonNull J.FieldAccess fieldAccess, @NonNull ExecutionContext ctx) {
        J.FieldAccess f = (J.FieldAccess) super.visitFieldAccess(fieldAccess, ctx);
        if (isTypeAccess(f)) return f;
        if (!isJoda(f.getType())) return f;

        JavaType javaTimeType = timeClassMap.getJavaTimeType(f.getType());
        if (javaTimeType == null) return f;

        String javaTimeShortName = timeClassMap.getJavaTimeShortName(f.getType());
        if (javaTimeShortName == null) return f;

        J.Identifier fieldName = f.getName();

        JavaFieldTemplate fieldTemplate = AllTemplates.getFieldTemplate(f);
        if(fieldTemplate != null) {
            javaTimeType = JavaType.buildType(fieldTemplate.getType());
            javaTimeShortName = fieldTemplate.getSimpleName();
            fieldName = fieldName.withSimpleName(fieldTemplate.getName());
        }

        Expression target = f.getTarget();
        if (target instanceof J.Identifier) {
            if (javaTimeShortName != null) {
                target = ((J.Identifier) target).withSimpleName(javaTimeShortName).withType(javaTimeType);
            }
        }

        return f.withType(javaTimeType).withName(fieldName).withTarget(target);
    }
    private  boolean isTypeAccess(J.FieldAccess f) {
        return f.getName().getFieldType() == null;
    }

    @Override
    public @NonNull J visitIdentifier(@NonNull J.Identifier ident, @NonNull ExecutionContext ctx) {
        J.Identifier i = (J.Identifier) super.visitIdentifier(ident, ctx);
        if (i.getFieldType() == null) return i;
        if (!isJoda(i.getFieldType().getType())) return i;

        JavaType type = i.getType();
        if (type instanceof JavaType.Array) {return visitArrayIdentifier(i, ctx);}
        if (type instanceof JavaType.Class) {return visitClassIdentifier(i, ctx);}

        return i;
    }
    private @NonNull J visitArrayIdentifier(J.Identifier arrayIdentifier, @NonNull ExecutionContext ctx) {
        JavaType.Array at = (JavaType.Array)arrayIdentifier.getType();
        JavaType.Array javaTimeType = at.withElemType(timeClassMap.getJavaTimeType(at.getElemType()));
        if (javaTimeType == null) return arrayIdentifier;

        return arrayIdentifier.withType(javaTimeType)
                .withFieldType(arrayIdentifier.getFieldType().withType(javaTimeType));
    }
    private @NonNull J visitClassIdentifier(J.Identifier classIdentifier, @NonNull ExecutionContext ctx) {
        JavaType javaTimeType = timeClassMap.getJavaTimeType(classIdentifier.getType());
        if (javaTimeType == null) return classIdentifier;

        return classIdentifier.withType(javaTimeType)
                .withFieldType(classIdentifier.getFieldType().withType(javaTimeType));
    }

    @Override
    public J visitArrayAccess(J.ArrayAccess arrayAccess, @NonNull ExecutionContext ctx){
        J.ArrayAccess a = (J.ArrayAccess) super.visitArrayAccess(arrayAccess, ctx);
        if (!isJoda(a.getType())) return a;

        JavaType javaTimeType = timeClassMap.getJavaTimeType(a.getType());
        if (javaTimeType == null) return a;

        return a.withType(javaTimeType);
    }

    private J migrateMethodCall(MethodCall original, MethodCall updated, @NonNull ExecutionContext ctx) {
        if (original.getMethodType() == null || !isJoda(original.getMethodType().getDeclaringType())) {
            return updated;
        }
        MethodTemplate template = AllTemplates.getTemplate(original);
        if (template == null) {
            logger.info(JODA_MISSING_MAPPING, original);
            return original; // unhandled case
        }

        if (JODA_MULTIPLE_MAPPING_POSSIBLE.equals(template.getTemplate().getCode())) {
            logger.info(JODA_MULTIPLE_MAPPING_POSSIBLE, original);
        }
        if (JODA_NO_AUTOMATIC_MAPPING_POSSIBLE.equals(template.getTemplate().getCode())) {
            return original; // usage with no automated mapping
        }
        if (JODA_NO_AUTOMATIC_MAPPING_POSSIBLE.equals(template.getTemplate().getCode())) {
            logger.info(JODA_NO_AUTOMATIC_MAPPING_POSSIBLE, original);
            return original; // usage with no automated mapping
        }

        J maybeUpdated = applyTemplate(original, updated, template);
        if (maybeUpdated == null) {
            logger.info("Can not apply template: " + template, original);
            return original; // unhandled case
        }

        Expression updatedExpr = (Expression) maybeUpdated;
        if (!isArgument(original)) {
            return updatedExpr;
        }

        // this expression is an argument to a method call
        MethodCall parentMethod = getCursor().getParentTreeCursor().getValue();
        JavaType.Method parentMethodType = parentMethod.getMethodType();
        if (isJoda(parentMethodType.getDeclaringType())) {
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
        JavaType updatedReturnType = timeClassMap.getJavaTimeType(returnType);
        if (updatedReturnType == null) {
            return original; // unhandled case
        }
        return updated.withMethodType(updated.getMethodType().withReturnType(updatedReturnType))
                .withName(updated.getName().withType(updatedReturnType));
    }

    private J applyTemplate(MethodCall original, MethodCall updated, MethodTemplate template) {
        if (template.getMatcher().matches(original)) {
            Expression[] args = template.getTemplateArgsFunc().apply(updated);
            if (args.length == 0) {
                return template.getTemplate().apply(updateCursor(updated), updated.getCoordinates().replace());
            }
            return template.getTemplate().apply(updateCursor(updated), updated.getCoordinates().replace(), (Object[]) args);
        }
        return null;
    }

    private boolean isJoda(JavaType type) {
        if (type == null) return false;

        if (type.isAssignableFrom(JODA_CLASS_PATTERN)) return true;

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
